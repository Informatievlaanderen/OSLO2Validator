<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isELIgnored="false" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="nl">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <script src="./js/jquery-1.3.2.js" type="text/javascript"></script>

        <script>
            $(document).ready(function () {

                console.log('hola');
                var target = document.getElementById("doc_container");

                var observer = new MutationObserver(function (mutations) {
                    if ($("#deactivate").css('display') == "none") {
                        $("#deactivate").css('display', 'block');
                    } else {
                        $("#deactivate").css('display', 'none');
                    }
                });

                // configuration of the observer:
                var config = {attributes: true, childList: true, characterData: true};

                // pass in the target node, as well as the observer options
                observer.observe(target, config);
            });
        </script>
    </head>
    <body>
        <jsp:include page="header.jsp"></jsp:include>

            <!-- Start page content -->

            <div class="main-content" id="main" itemprop="mainContentOfPage" role="main" tabindex="-1">
                <section class="region">
                    <div class="layout layout--wide" id="form">

                        <div class="grid" data-tabs data-tabs-responsive-label="Navigatie">
                            <div class="col--1-1">
                                <div class="tabs__wrapper">
                                    <ul class="tabs" data-tabs-list>
                                        <li class="tab tab--active">
                                            <a class="tab__link" id="tab-2-nummer-1" href="#" data-tab>Valideer via opladen van bestand</a>
                                        </li>
                                        <li class="tab">
                                            <a class="tab__link" id="tab-2-nummer-2" href="#" data-tab>Valideer via URL</a>
                                        </li>
                                        <li class="tab">
                                            <a class="tab__link" id="tab-2-nummer-3" href="#" data-tab>Valideer met eigen SHACL file</a>
                                        </li>
                                    </ul>
                                </div>
                            </div>

                            <section class="col--1-1 tab__pane" data-tab-pane>

                                <h1 class="h1">Valideer via opladen van bestand</h1>
                                <div class="typography">
                                    <!--Put Content for first tab here-->
                                    <form method="POST" action="validate" enctype="multipart/form-data">
                                        <h2 class="h2">OSLO² applicatie profiel</h2>
                                        Selecteer hieronder het OSLO² applicatie profiel waartegen u uw data wil valideren.
                                        </br>
                                        <select class="select" name="shapes">
                                        <c:forEach items="${options}" var="option">
                                            <option>${option}</option>
                                        </c:forEach>
                                    </select>
                                    <h2 class="h2">Data</h2>
                                    Selecteer hieronder het bestand dat u wil valideren.</br>

                                    <!-- component -->
                                    <div class="upload js-upload"
                                         data-upload-t-close="Sluiten" id="doc_container"
                                         >
                                        <div class="upload__element">
                                            <input class="upload__element__input" type="file" id="data" name="data"
                                                   data-upload-error-message-filesize="Het bestand mag max :maxFsz zijn."
                                                   data-upload-max-size="20000000" accept=".ttl, .rdf, .xml, .json, .jsonld" />
                                            <label class="upload__element__label" for="data">
                                                <i class="vi vi-paperclip"></i><span>Bijlage toevoegen</span>
                                            </label>
                                        </div>
                                        <div id='deactivate' style='cursor:not-allowed; width:100%; height:52px; background-color:#99999925; position:relative; bottom:51px; z-index:10; display:none;'></div>
                                    </div>
                                    <!-- end component -->

                                    <h2 class="h2">Valideer</h2>
                                    <input type="submit" class="button" value="Klik hier om te valideren" name="upload" id="upload" />
                                </form>
                            </div>
                        </section>

                        <section class="col--1-1 tab__pane" data-tab-pane>
                            <h1 class="h1">Valideer via URL</h1>
                            <div class="typography">
                                <!--Put Content for second tab here-->
                                <form method="POST" action="validate" enctype="multipart/form-data">
                                    <h2 class="h2">OSLO² applicatie profiel</h2>
                                    Selecteer hieronder het OSLO² applicatie profiel waartegen u uw data wil valideren.
                                    </br>
                                    <select class="select" name="shapes">
                                        <c:forEach items="${options}" var="option">
                                            <option>${option}</option>
                                        </c:forEach>
                                    </select>
                                    <h2 class="h2">Data</h2>
                                    Geef hieronder de URL op waarop de data gepubliceerd is die u wilt valideren.</br>

                                    <!-- input-field component -->          
                                    <label for="input-field" class="form__label">URI </label>
                                    <input class="input-field" id="input-field" type="text" placeholder="" name="dataURI" />

                                    </br><p></p>
                                    Geef hieronder eventuele optionele headers mee.
                                    </br><p></p>
                                    <!-- input-field component -->          
                                    <label for="input-field" class="form__label">Header key </label>
                                    <input class="input-field" id="input-field" type="text" placeholder="" name="headerKey" />
                                    <!-- input-field component -->          
                                    <label for="input-field" class="form__label">Header value </label>
                                    <input class="input-field" id="input-field" type="text" placeholder="" name="headerValue" />

                                    <h2 class="h2">Valideer</h2>
                                    <input type="submit" class="button" value="Klik hier om te valideren" name="upload" id="upload" />
                                </form>
                            </div>
                        </section>

                        <section class="col--1-1 tab__pane" data-tab-pane>

                            <h1 class="h1">Valideer met eigen SHACL file</h1>
                            <div class="typography">                                
                                Hier is het mogelijk om uw data te laten valideren tegen een SHACL file die geen applicatieprofiel is en door uzelf opgesteld mag zijn.
                                <br></br>
                                <!--Put Content for first tab here-->
                                <form method="POST" action="validate" enctype="multipart/form-data">
                                    <h2 class="h2">Data</h2>
                                    Selecteer hieronder het bestand dat u wil valideren.</br>

                                    <!-- component -->
                                    <div class="upload js-upload"
                                         data-upload-t-close="Sluiten" id="doc_container"
                                         >
                                        <div class="upload__element">
                                            <input class="upload__element__input" type="file" id="dataFile" name="dataFile"
                                                   data-upload-error-message-filesize="Het bestand mag max :maxFsz zijn."
                                                   data-upload-max-size="20000000" accept=".ttl, .rdf, .xml, .json, .jsonld" />
                                            <label class="upload__element__label" for="dataFile">
                                                <i class="vi vi-paperclip"></i><span>Bijlage toevoegen</span>
                                            </label>
                                        </div>
                                        <div id='deactivate' style='cursor:not-allowed; width:100%; height:52px; background-color:#99999925; position:relative; bottom:51px; z-index:10; display:none;'></div>
                                    </div>
                                    <!-- end component -->
                                    
                                    
                                    <h2 class="h2">SHACL </h2>
                                    Selecteer hieronder het SHACL-bestand waartegen u uw data wil valideren
                                    
                                    <!-- component -->
                                    <div class="upload js-upload"
                                         data-upload-t-close="Sluiten" id="doc_container"
                                         >
                                        <div class="upload__element">
                                            <input class="upload__element__input" type="file" id="shapeFile" name="shapeFile"
                                                   data-upload-error-message-filesize="Het bestand mag max :maxFsz zijn."
                                                   data-upload-max-size="20000000" accept=".ttl, .rdf, .xml, .json, .jsonld" />
                                            <label class="upload__element__label" for="shapeFile">
                                                <i class="vi vi-paperclip"></i><span>Bijlage toevoegen</span>
                                            </label>
                                        </div>
                                        <div id='deactivate' style='cursor:not-allowed; width:100%; height:52px; background-color:#99999925; position:relative; bottom:51px; z-index:10; display:none;'></div>
                                    </div>
                                    <!-- end component -->
                                    
                                    

                                    <h2 class="h2">Valideer</h2>
                                    <input type="submit" class="button" value="Klik hier om te valideren" name="upload" id="upload" />
                                </form>
                            </div>
                        </section>

                    </div>

                    </br>
                    </br>


                </div>
            </section>
        </div>

    </div>

    <jsp:include page="footer.jsp"></jsp:include>

</body>
</html>
