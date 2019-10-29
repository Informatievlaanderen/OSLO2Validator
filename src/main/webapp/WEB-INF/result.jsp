<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" import="validator.OSLO2.ValidationResult.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isELIgnored="false" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <link href="./media/dataTables/demo_page.css" rel="stylesheet" type="text/css" />
        <link href="./media/dataTables/demo_table.css" rel="stylesheet" type="text/css" />
        <link href="./media/dataTables/demo_table_jui.css" rel="stylesheet" type="text/css" />
        <link href="./media/themes/base/jquery-ui.css" rel="stylesheet" type="text/css" />
        <!--  <link href="./media/themes/smoothness/jquery-ui-1.7.2.custom.css" rel="stylesheet" type="text/css" media="all" /> -->
        <script src="./js/jquery-1.3.2.js" type="text/javascript"></script>
        <script src="./js/jquery.dataTables.min.js" type="text/javascript"></script>
        <script>
            $(document).ready(function () {
                $("#resultaten").dataTable({
                    "sPaginationType": "full_numbers",
                    "bJQueryUI": true
                });
            });
        </script>
    </head>
    <body>

        <jsp:include page="header.jsp"></jsp:include>

            <div class="main-content" id="main" itemprop="mainContentOfPage" role="main" tabindex="-1">
                <section class="region">
                    <div class="layout layout--wide">

                        <form action="home">
                            <input type="submit" class="button" value="Terug" />
                        </form>

                        <div class="grid" data-tabs data-tabs-responsive-label="Navigatie">
                            <div class="col--1-1">
                                <div class="tabs__wrapper">
                                    <ul class="tabs" data-tabs-list>
                                        <li class="tab tab--active">
                                            <a class="tab__link" id="tab-2-nummer-1" href="#" data-tab>Resultaat in TTL</a>
                                        </li>
                                        <li class="tab">
                                            <a class="tab__link" id="tab-2-nummer-2" href="#" data-tab>Resultaat in tabel</a>
                                        </li>
                                        <li class="tab">
                                            <a class="tab__link" id="tab-2-nummer-3" href="#" data-tab>Data</a>
                                        </li>
                                        <li class="tab">
                                            <a class="tab__link" id="tab-2-nummer-4" href="#" data-tab>Regels</a>
                                        </li>
                                    </ul>
                                </div>
                            </div>

                            <section class="col--1-1 tab__pane" data-tab-pane>
                                <h1 class="h1">Resultaat in TTL</h1>
                                Hieronder ziet u een overzicht van de validatieresultaten.
                                <br><br>
                                <div class="typography">
                                    <!--Put Content for first tab here-->
                                    <pre><c:out value="${report.result != null ? report.result : 'no report found'}"/></pre> 
                            </div>
                        </section>

                        <section class="col--1-1 tab__pane" data-tab-pane>
                            <h1 class="h1">Resultaat in tabel</h1>
                            Hieronder ziet u overzicht van de validatieresultaten.
                            <div class="typography">
                                <!--Put Content for second tab here-->
                                <table id="resultaten" class="display">
                                    <thead>
                                        <tr>
                                            <th>Focus Node</th>
                                            <th>Result Message</th>
                                            <th>Result Path</th>
                                            <th>Result Severity</th>
                                            <th>Source Constraint</th>
                                            <th>Source Constraint Component</th>
                                            <th>Source Shape</th>
                                            <th>Value</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${report.validationResultList}" var="result">
                                            <tr>
                                                <td>${result.focusNode}</td>
                                                <td>${result.resultMessage}</td>
                                                <td>${result.resultPath}</td>
                                                <td>${result.resultSeverity}</td>
                                                <td>${result.sourceConstraint}</td>
                                                <td>${result.sourceConstraintComponent}</td>
                                                <td>${result.sourceShape}</td>
                                                <td>${result.value}"</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </section>

                        <section class="col--1-1 tab__pane" data-tab-pane>
                            <h1 class="h1">Data</h1>
                            <c:choose>
                                <c:when test="${shaclSent == true}">
                                    Hieronder ziet u een overzicht van de data die gevalideerd werd. 
                                    <br />
                                </c:when>    
                                <c:otherwise>
                                    Hieronder ziet u een overzicht van de data die gevalideerd werd. Aan de data die u opgaf werd het betreffende vocabularium aan toegevoegd om meer context aan uw bestand toe te voegen en het validatieresultaat te verbeteren.
                                    <br />
                                </c:otherwise>
                            </c:choose>
                            </br></br>
                            <div class="typography">
                                <!--Put Content for third tab here-->
                                <pre><c:out value="${report.data != null ? report.data : 'no report found'}"/></pre>
                            </div>
                        </section>

                        <section class="col--1-1 tab__pane" data-tab-pane>
                            <h1 class="h1">Regels</h1>
                            <c:choose>
                                <c:when test="${shaclSent == true}">
                                    Hieronder ziet u een overzicht van de SHACL regels die gebruikt werden om uw data te valideren.
                                    <br />
                                </c:when>    
                                <c:otherwise>
                                    Hieronder ziet u een overzicht van de SHACL regels die gebruikt werden om uw data te valideren. Deze SHACL regels zijn gebasseerd op het applicatie profiel dat u selecteerde in de eerste stap.
                                    <br />
                                </c:otherwise>
                            </c:choose>
                            </br></br>
                            <div class="typography">
                                <!--Put Content for fourth tab here-->
                                <pre><c:out value="${report.shapes != null ? report.shapes : 'no report found'}"/></pre>
                            </div>
                        </section>

                    </div>


                </div>
            </section>
        </div>

    </div>

    <jsp:include page="footer.jsp"></jsp:include>

</body>
</html>