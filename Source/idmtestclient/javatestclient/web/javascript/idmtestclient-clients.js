$(document).ready(function(){
    
    //getClientsBtn onClick()
    $('#getClientsBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#clients_customerNumber').val();
        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }

        $('#status_get_clients').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/getClients.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber)
            },
            success: function(resp) {
                try {
                    var statusCode = $AjaxHelper.getStatusCode(resp);
                    var result = $AjaxHelper.getResult(resp);

                    $('#status_get_clients').text(statusCode);
                    $('#get_clients_response').val(result);
                }
                catch(err) {
                    alert("Error setting response");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#get_clients_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getClientsBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // registerClientBtn onClick()
    $('#registerClientBtn').click(function() {

        $('#status_register_client').text("");
        $(this).attr("disabled", true);

        var accessToken = $('#access_token').val();
        var customerNumber = $('#clients_customerNumber').val();
        var clientName = $('#clients_clientName').val();

        var url = API_URL + "/registerClient.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                "accessToken": $.URLEncode(accessToken),
                "customerNumber": $.URLEncode(customerNumber),
                "clientName": $.URLEncode(clientName)
            },
            success: function(resp) {
                try {
                    var statusCode = $AjaxHelper.getStatusCode(resp);
                    var result = $AjaxHelper.getResult(resp);

                    $('#status_register_client').text(statusCode);
                    $('#register_client_response').val(result);
                }
                catch(err) {
                    alert("Error setting response");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#register_client_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#registerClientBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // getClientBtn onClick()
    $('#getClientBtn').click(function() {

        $('#status_clients_response').text("");
        $(this).attr("disabled", true);

        var accessToken = $('#access_token').val();
        var customerNumber = $('#clients_customerNumber').val();
        var clientId = $('#clients_clientId').val();

        var url = API_URL + "/getClient.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                "accessToken": $.URLEncode(accessToken),
                "customerNumber": $.URLEncode(customerNumber),
                "clientId": $.URLEncode(clientId)
            },
            success: function(resp) {
                try {
                    var statusCode = $AjaxHelper.getStatusCode(resp);
                    var result = $AjaxHelper.getResult(resp);

                    $('#status_clients_response').text(statusCode);
                    $('#clients_response').val(result);
                }
                catch(err) {
                    alert("Error setting response");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_clients_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getClientBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // getClientPermissionsBtn onClick()
    $('#getClientPermissionsBtn').click(function() {

        $('#status_clients_response').text("");
        $(this).attr("disabled", true);

        var accessToken = $('#access_token').val();
        var customerNumber = $('#clients_customerNumber').val();
        var clientId = $('#clients_clientId').val();

        var url = API_URL + "/getClientPermissions.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                "accessToken": $.URLEncode(accessToken),
                "customerNumber": $.URLEncode(customerNumber),
                "clientId": $.URLEncode(clientId)
            },
            success: function(resp) {
                try {
                    var statusCode = $AjaxHelper.getStatusCode(resp);
                    var result = $AjaxHelper.getResult(resp);

                    $('#status_clients_response').text(statusCode);
                    $('#clients_response').val(result);
                }
                catch(err) {
                    alert("Error setting response");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_clients_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getClientPermissionsBtn').attr("disabled", false);
                }, 500);
            }
        });
    });
});