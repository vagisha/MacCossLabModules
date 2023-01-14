<%
    /*
     * Copyright (c) 2008-2019 LabKey Corporation
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("internal/jQuery");
    }
%>
<%
    JspView<PanoramaPublicController.CatalogEntryBean> view = (JspView<PanoramaPublicController.CatalogEntryBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();

    var attachedFile = bean.getImageFileName();
    var attachmentUrl = bean.getImageUrl();

    ActionURL cancelUrl = PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), form.getContainer());

%>
<labkey:errors/>


<div style="margin-top:15px;">
    <!-- <form id="catalogEntryForm" method="post" enctype="multipart/form-data">-->
    <form id="catalogEntryForm" method="post" enctype="multipart/form-data">
        <labkey:csrf />
        <%=generateReturnUrlFormField(form.getReturnActionURL())%>
        <table class="lk-fields-table">
            <tr>
                <td class="labkey-form-label">Description:</td>
                <td><textarea id="descFieldInput" rows="5" cols="60" name="datasetDescription" ><%=h(form.getDatasetDescription())%></textarea></td>
            </tr>
            <tr>
                <td class="labkey-form-label">Image:</td>
                <td>
                    <input id="imageFileInput" type="file" size="50" style="border: none; background-color: transparent;" accept="image/*">
                        <%=h(attachedFile)%>
                    </input>
                </td>
            </tr>
            <tr>
                <td>
                    Name: <input id="imageFileName" name="imageFileName" type="hidden" value="<%=h(form.getImageFileName())%>"/>
                    URL: <input id="modifiedImage" name="imageFile" type="hidden" />
                </td>
            </tr>
        </table>
        <br>
        <%=button("Submit").onClick("submitEntry(); return false;")%>
        &nbsp
        <%=button("Cancel").href(cancelUrl)%>
    </form>
</div>

<div id="cropperDiv" style="display:none;">
    <canvas id="canvas">
        Your browser does not support the HTML5 canvas element.
    </canvas>
    <input type="button" id="btnCrop" value="Crop" />
    <input type="button" id="btnRestore" value="Restore" />
</div>
<div id="result"></div>

<!-- <link rel="stylesheet" href="https://unpkg.com/bootstrap@4/dist/css/bootstrap.min.css" crossorigin="anonymous">
<script src="https://unpkg.com/bootstrap@4/dist/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script> -->
<!-- <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.5.13/cropper.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.5.13/cropper.min.js"></script> -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/cropper/2.3.3/cropper.css"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/cropper/2.3.3/cropper.js"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-cropper/1.0.1/jquery-cropper.min.js"></script>
<style>
    .img-container img {
        max-width: 100%;  /* This rule is very important, please do not ignore this! */
    }
    img {
        max-width: 100%;
    }

    #canvas {
        height: 600px;
        width: 600px;
        background-color: #ffffff;
        cursor: default;
        border: 1px solid black;
    }
</style>
<script type="text/javascript">

    $(document).ready(function() {

        let cropper;
        let croppedImageDataUrl;

        const canvas  = $("#canvas");
        const context = canvas.get(0).getContext("2d");
        const result = $('#result');

        $('#imageFileInput').on( 'change', function(){
            if (this.files && this.files[0]) {
                if ( this.files[0].type.match(/^image\//) ) {
                    const fileName = this.files[0].name;
                    alert("Uploaded file size: " + this.files[0].size + ", Name: " + fileName);
                    $("#imageFileName").val(fileName);

                    const reader = new FileReader();
                    reader.onload = function(evt) {
                        $("#cropperDiv").show();

                        const img = new Image();
                        img.onload = function() {
                            context.canvas.height = img.height;
                            context.canvas.width  = img.width;
                            context.drawImage(img, 0, 0);
                            cropper = canvas.cropper({
                                aspectRatio: 3 / 2
                            });
                            $('#btnCrop').click(function() {
                                // Get a string base 64 data url
                                croppedImageDataUrl = canvas.cropper('getCroppedCanvas').toDataURL("image/png");
                                result.append( $('<img>').attr('src', croppedImageDataUrl) );
                                // Set the modified image to be content when the form is submitted.
                                $("#modifiedImage").val(croppedImageDataUrl);
                            });
                            $('#btnRestore').click(function() {
                                canvas.cropper('reset');
                                result.empty();
                            });
                        };
                        img.src = evt.target.result;
                    };
                    reader.readAsDataURL(this.files[0]);
                }
                else {
                    alert("Invalid file type! Please select an image file.");
                }
            }
            else {
                alert('No file(s) selected.');
            }
        });

    });

    function submitEntry()
    {
        // alert("Submitting form");
        var description = $("#descFieldInput").val();
        console.log("description entered: " + description);
        $("#catalogEntryForm").submit();
        return;

        <%--var formData = new FormData();--%>
        <%--formData.append("datasetDescription", description);--%>
        <%--formData.append("X-LABKEY-CSRF", LABKEY.CSRF);--%>
        <%--formData.append("returnUrl", <%=form.getReturnActionURL()%>)--%>
        <%--$.ajax( {--%>
        <%--    url: <%=q(getActionURL())%>,--%>
        <%--    method: 'POST',--%>
        <%--    form: formData,--%>
        <%--    contentType: false,--%>
        <%--    processData: false,--%>
        <%--    success: function (result) {--%>
        <%--        alert(result.message);--%>
        <%--    },--%>
        <%--    async: true--%>
        <%--})--%>
    }
</script>