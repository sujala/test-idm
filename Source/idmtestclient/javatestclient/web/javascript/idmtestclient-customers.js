$(document).ready(function(){

    // lockCustomerBtn onClick()
    $('#lockCustomerBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#customerNumber').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }

        $('#status_customer_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/lockCustomer.jsp";
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

                    $('#status_customer_response').text(statusCode);
                    $('#customer_response').val(result);
                }
                catch(err) {
                    alert("Error setting response");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_customer_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#lockCustomerBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // unlockCustomerBtn onClick()
    $('#unlockCustomerBtn').click(function() {

        var accessToken = $('#access_token').val();
        var customerNumber = $('#customerNumber').val();
        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }

        $('#status_customer_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/unlockCustomer.jsp";
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

                    $('#status_customer_response').text(statusCode);
                    $('#customer_response').val(result);
                }
                catch(err) {
                    alert("Error setting response");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_customer_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#unlockCustomerBtn').attr("disabled", false);
                }, 500);
            }
        });
    });
});