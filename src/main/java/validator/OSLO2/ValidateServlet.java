package validator.OSLO2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.util.FileUtils;
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.spin.util.JenaUtil;

/**
 * Servlet implementation class validateServlet
 */
@WebServlet
@MultipartConfig
public class ValidateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static Log logger = LogFactory.getFactory().getInstance(ValidateServlet.class);
    private Configuration config;
    private LoadingCache<String, List<Model>> cache;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ValidateServlet() {
        super();
        try {
            config = Configuration.loadFromEnvironment();
            logger.info("Using configuration " + config);
        } catch (IllegalArgumentException e) {
            logger.fatal(e.getMessage());
            System.exit(1);
        }
        cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, List<Model>>() {
                    @Override
                    public List<Model> load(String s) throws Exception {
                        Model shaclModel = JenaUtil.createMemoryModel();
                        Model vocModel = JenaUtil.createMemoryModel();
                        Map<String, APModel> aps = config.getApplicationProfiles();
                        APModel ap = aps.get(s);
                        shaclModel.read(ap.getLocation(), "TURTLE");
                        ap.getDependencies().forEach(voc -> vocModel.read(voc, "TURTLE"));
                        return Arrays.asList(shaclModel, vocModel);
                    }
                });
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Validate the data and write to validationReport
        ValidationReport validationReport = validate(request);

        // Store validationReport to the request attribute before forwarding
        request.setAttribute("report", validationReport);
        
        // Add extra attribute to indicate if an own SHACL file was sent (just to display other text in result)
        if(request.getPart("shapeFile") == null){
            request.setAttribute("shaclSent", false);            
        } else {
            request.setAttribute("shaclSent", true);
        }

        // Forward to /WEB-INF/views/validatedView.jsp
        RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/WEB-INF/result.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * Validate the data and return the validationReport TODO: this should be
     * refactored into its own class. There are requests to have this available
     * as a local cli application
     *
     * @param request The information provided with the request.
     * @throws ServletException
     * @throws IOException
     */
    private ValidationReport validate(HttpServletRequest request) throws IOException, ServletException {
        Model shaclModel;
        Model vocModel;

        if (request.getPart("shapeFile") != null) { // An own SHACL file was sent (tab 3)
            InputStream shaclStream = request.getPart("shapeFile").getInputStream();
            String filename = getSubmittedFileName(request.getPart("shapeFile"));
            String extension = checkForFileLang(filename);

            shaclModel = JenaUtil.createMemoryModel();
            shaclModel.read(shaclStream, null, extension);

            vocModel = null;

        } else { // When an Application Profile was selected (tab 1 and 2)

            //Get option selected in the drop down
            String shapesOption = IOUtils.toString(request.getPart("shapes").getInputStream(), "UTF-8");

            try {
                List<Model> models = cache.get(shapesOption);
                shaclModel = models.get(0);
                vocModel = models.get(1);
            } catch (ExecutionException e) {
                throw new IOException(e.getCause());
            }
        }

        // Get data to validate from file, and combine with the vocabulary
        // Check whether a file or a URI was provided.
        Model dataModel = getDataModel(request, shaclModel, vocModel);

        // Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
        Resource resource = ValidationUtil.validateModel(dataModel, shaclModel, false);
        Model reportModel = resource.getModel();
        reportModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

        // Get the ttl Result and Table Result
        List<ValidationResult> validationResultsList = formatOutput(reportModel);
        String ttlResult = ModelPrinter.get().print(reportModel);

        // Get the data and SHACL as string from their models
        String data = getStringFromModel(dataModel);
        String shapes = getStringFromModel(shaclModel);

        // Create the validationReport, including the result, the data validated, and the rules used during validation
        return new ValidationReport(validationResultsList, ttlResult, data, shapes);

    }

    /**
     * Get data to validate from file, and combine with the vocabulary
     *
     * @param request The client http request containing the data to be
     * validated
     * @param shapesModel The Jena model containing the shacl defintion (needed
     * to set the proper prefixes on the input data)
     * @param vocModel The vocabulary associated with the shacl definition.
     * These terms need to be added to the input data for the reasoner to be
     * able to do its job
     * @throws IOException
     * @throws ServletException
     */
    private Model getDataModel(HttpServletRequest request, Model shapesModel, Model vocModel) throws IOException, ServletException {
        InputStream dataStream;
        String fileName;
        String extension;

        // Check whether a file or a URI was provided. And determine it's extension.
        if (request.getPart("data") == null && request.getPart("dataFile") == null) {
            logger.debug("No payload provided, trying to get data from URL");
            // If URI, do GET
            URLConnection httpCon = new URL(request.getParameter("dataURI")).openConnection();
            logger.debug("url: " + httpCon.getURL().toString() + ".");
            if (request.getPart("headerKey") != null && !"".equals(request.getParameter("headerKey"))) {
                httpCon.setRequestProperty(request.getParameter("headerKey"), request.getParameter("headerValue"));
            }
            // TODO: catch the IO exception and render a pretty error page
            dataStream = httpCon.getInputStream();
            extension = RDFLanguages.contentTypeToLang(ContentType.create(httpCon.getContentType())).getName();

            // If extension not found in Content-Type header
            if (extension == null) {
                String urlString = request.getParameter("dataURI");
                URL url = new URL(urlString);
                extension = FilenameUtils.getExtension(url.getPath());
            }

        } else {
            // If file
            Part dataPart;
            if (request.getPart("data") == null) {
                dataPart = request.getPart("dataFile"); // Data from tab with own shacl file
            } else {
                dataPart = request.getPart("data"); // Data from tab with file uploading
            }

            dataStream = dataPart.getInputStream();
            fileName = getSubmittedFileName(dataPart);
            extension = checkForFileLang(fileName);
        }

        // Upload the data in the Model. First set the prefixes of the model to those of the shapes model to avoid mismatches.
        Model dataModel = JenaUtil.createMemoryModel();
        dataModel.setNsPrefixes(shapesModel.getNsPrefixMap());
        dataModel.read(dataStream, null, extension);

        // Add vocabulary terms to the data model - (when a OSLO Application Profile was selected, so not an own SHACL file was sent)
        if (request.getPart("data") != null) {
            dataModel.add(vocModel);
        }

        return dataModel;
    }

    /**
     * Method filters the given result using sparql and provides byte array
     * stream containing the result of the sparql query using the provided
     * format. The different formats are created using standard jena
     * ResultSetFormatter
     *
     * @param result Result from shacl validation
     * @param queryStr String containing sparql query
     * @param format String switching output format, currently only provided
     * TXT, CSV, XML
     * @return ByteArrayOutputStream
     * @throws IOException Writing to stream might cause problems
     */
    private static ByteArrayOutputStream formatOutput(Model result, String queryStr, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Query query = QueryFactory.create(queryStr);

        QueryExecution qe = QueryExecutionFactory.create(query, result);

        ResultSet queryResult = qe.execSelect();

        queryResult.getResourceModel().setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        if (format.matches("(CSV)")) {
            ResultSetFormatter.output(outputStream, queryResult, ResultsFormat.FMT_RS_CSV);
        } else if (format.matches("(XML)")) {
            ResultSetFormatter.outputAsXML(outputStream, queryResult);
        } else if (format.matches("(TTL)")) {
            ResultSetFormatter.output(outputStream, queryResult, ResultsFormat.FMT_RDF_TTL);
        } else {
            // output txt
            String resultOutput = ResultSetFormatter.asText(queryResult);
            if (resultOutput != null) {
                outputStream.write(resultOutput.getBytes());
            }
        }
        // close this resource
        qe.close();

        return outputStream;

    }

    /**
     * Method to create a list of validation results from the validationModel
     *
     * @param model The ValidationModel which should be converted
     * @throws IOException Writing to stream might cause problems
     */
    private static List<ValidationResult> formatOutput(Model model) throws IOException {
        // formating result using query to CSV
        InputStream is = ValidateServlet.class.getResourceAsStream("/defaultquery.rq");
        String queryStr = FileUtils.readWholeFileAsUTF8(is);
        ByteArrayOutputStream resultStream = formatOutput(model, queryStr, "CSV");
        String output = resultStream.toString();
        resultStream.close();

        // Split the CSV string on newline character, add to ArrayList and remove empty lines
        List<String> linesList = Arrays.asList(output.split("[\\n\\r]"));
        ArrayList<String> lines = new ArrayList<String>();
        lines.addAll(linesList);
        lines.removeAll(Arrays.asList(""));

        // Replace empty values with NA. 
        // If last value is empty, this cannot be changed. It is handled later
        List<List<String>> items = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            while (lines.get(i).contains(",,")) {
                lines.set(i, lines.get(i).replaceAll(",,", ",NA,"));
            }
        }

        // Split each line on "," and if the last value was empty, complete with NA
        List<String> firstLineSplit = new ArrayList<>(Arrays.asList((lines.get(0).split("\\s*,\\s*"))));
        int size = firstLineSplit.size();
        for (int i = 0; i < lines.size(); i++) {
            List<String> splittedLine = new ArrayList<>(Arrays.asList((lines.get(i).split("\\s*,\\s*"))));
            if (splittedLine.size() != size) {
                splittedLine.add("NA");
            }
            items.add(splittedLine);
        }

        // Load into a List of ValidationResults
        List<ValidationResult> validationList = new ArrayList<ValidationResult>();
        //Start at 1 because the first row contains the headers and we do not want to include those.
        for (int j = 1; j < items.size(); j++) {
            ValidationResult validationResult = new ValidationResult();
            validationResult.setFocusNode(items.get(j).get(0));
            validationResult.setResultMessage(items.get(j).get(1));
            validationResult.setResultPath(items.get(j).get(2));
            validationResult.setResultSeverity(items.get(j).get(3));
            validationResult.setSourceConstraint(items.get(j).get(4));
            validationResult.setSourceConstraintComponent(items.get(j).get(5));
            validationResult.setSourceShape(items.get(j).get(6));
            validationResult.setValue(items.get(j).get(7));

            validationList.add(validationResult);

        }

        return validationList;
    }

    /**
     * Method to get the content of a model, in TTL
     *
     * @param model The model to get the content from
     */
    private static String getStringFromModel(Model model) {
        StringWriter out = new StringWriter();
        model.write(out, "TTL");
        String result = out.toString();

        return result;
    }

    /**
     * Get the name of the submitted file.
     */
    private static String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
            }
        }
        return null;
    }

    /**
     * Method checks file format simply by mapping the file extensions to
     * expected format.</br>
     * <ul>
     * <li>extension .ttl --> FileUtils.langTurtle.</li>
     * <li>extension .rdf --> FileUtils.langXML.</li>
     * <li>extension .xml --> FileUtils.langXML.</li>
     * <li>extension .jsonld --> JSONLD</li>
     * </ul>
     *
     * @param filename
     * @return String
     */
    private static String checkForFileLang(String filename) {
        if (filename == null) {
            return "";
        }
        if (filename.endsWith(".ttl")) {
            return FileUtils.langTurtle;
        } else if (filename.endsWith(".xml") || filename.endsWith(".rdf")) {
            return FileUtils.langXML;
        } else if (filename.endsWith(".jsonld") || filename.endsWith(".json")) {
            return "JSONLD";
        } else {
            return "";
        }
    }
}
