<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>Idm Test Client</title>
<link href="css/main.css" media="screen" rel="stylesheet" type="text/css" />
<link href="css/tab-menu.css" media="screen" rel="stylesheet" type="text/css" />
<link href="css/main.css" media="screen" rel="stylesheet" type="text/css" />

<script type="text/javascript" src="javascript/jquery-1.4.3.min.js"></script>
<script type="text/javascript" src="javascript/jquery-plugin-urlencode.js"></script>
<script type="text/javascript" src="javascript/tabmenu-events.js"></script>

<script type="text/javascript" src="javascript/idmtestclient.js"></script>
<script type="text/javascript" src="javascript/idmtestclient-tokens.js"></script>
<script type="text/javascript" src="javascript/idmtestclient-customers.js"></script>
<script type="text/javascript" src="javascript/idmtestclient-clients.js"></script>
<script type="text/javascript" src="javascript/idmtestclient-users.js"></script>
<script type="text/javascript" src="javascript/idmtestclient-password.js"></script>
</head>
<body>
<div id="content-wrapper" align="left">
    <img src="images/top-banner.png" width="906" height="188" border="0" alt="top banner"/>
    <div style="margin-left: 20px;">


<h2>IDM Test Client</h2>

<div id="getTokenErrorMsg"></div>
<div>
	Get Token:
	<select id="tokenType">
		<option value="">Choose Type</option>
		<option value="admin">Admin</option>
		<option value="app-billing">Billing App</option>
		<option value="app-cloudcp">Cloud CP App</option>
                <option value="signup-service">Signup Service</option>
		<option value="racker">Racker</option>
		<option value="non-admin">Non-Admin User</option>
                <option value="refresh-token">Refresh Token</option>
	</select>
</div>
<table id="getTokenContainer">
	<tr>
            <td>ClientId: </td>
            <td><input id="clientId" class="textinput"/></td>
            <td style="padding-left: 10px;">ClientSecret: </td>
            <td><input id="clientSecret" class="textinput"/></td>
	</tr>
	<tr>
            <td>Username:</td>
            <td><input id="username" class="textinput"/></td>
            <td style="padding-left: 10px;">Password:</td>
            <td><input id="password" class="textinput"/></td>
	</tr>
	<tr>
            <td>RefreshToken:</td>
            <td colspan="3"><input id="refreshToken" style="width: 500px;" /></td>
        </tr>
        <tr>
            <td colspan="4">
                <input type="button" id="getTokenBtn" value="getToken" style="width: 105px;" />&nbsp;
                <input id="access_token" class="token_field"/>
            </td>
	</tr>

</table>

<hr style="margin-right: 20px;" />

<div id="menu_container">

    <div style="float: left;">
            <div class="tab-menu-filler_a">&nbsp;</div>
            <ul class="menu">
                    <li id="menu_users" class="active">Users</li>
                    <li id="menu_clients">Clients</li>
                    <li id="menu_customers">Customers</li>
                    <li id="menu_password">Password</li>
                    <li id="menu_tokens">Tokens</li>
            </ul>
            <div class="tab-menu-filler_b">&nbsp;</div>
    </div>
    <div style="float: left;">
            <!-- Users tab -->
            <div class="menu_content menu_users">
                <h2>Users</h2>

                <div>
                    <table cellpadding="5">
                    <tr>
                        <td>CustomerNumber:</td>
                        <td><input id="users_customerNumber" class="textinput" value="rcn-000-000-000"/></td>
                    </tr>
                    </table>
                </div>
                <div style="margin-top: 10px;">
                    <input id="getUsersBtn" type="button" value="getUsers" />
                    <div style="margin-top: 10px;">
                        Response: <span id="status_getUsersResponse"></span><br/>
                        <textarea id="getUsersResponse" style="height: 100px;"></textarea>
                    </div>
                </div>

                <div style="margin-top: 10px;">
                    <table cellpadding="5">
                        <tr>
                            <td>Username:</td>
                            <td><input id="users_username" value="testuser2" /></td>
                            <td>Password:</td>
                            <td><input id="users_password" value="P@ssw0rd" /></td>
                        </tr>
                        <tr>
                            <td>FirstName:</td>
                            <td><input id="users_firstname" value="John"/></td>
                            <td>LastName:</td>
                            <td><input id="users_lastname" value="Smith"/></td>
                        </tr>
                        <tr>
                            <td>MiddleName:</td>
                            <td><input id="users_middlename" value="Paul"/></td>
                            <td>Email:</td>
                            <td><input id="users_email" value="jsmith@example.com"/></td>
                        </tr>
                        <tr>
                            <td>SecretQuestion:</td>
                            <td colspan="3"><input id="users_secretQuestion" value="What is the meaning of liff?" style="width: 405px;"/></td>
                        </tr>
                        <tr>
                            <td>SecretAnswer:</td>
                            <td colspan="3"><input id="users_secretAnswer" value="Forty-two"style="width: 405px;"/></td>
                        </tr>
                    </table>
                    <div style="margin-top: 10px;">
                        <input id="addFirstUserBtn" type="button" value="addFirstUser" />
                        <input id="addUserBtn" type="button" value="addUser" />
                        <input id="getUserBtn" type="button" value="getUser" />
                        <input id="updateUserBtn" type="button" value="updateUser" />
                        <input id="deleteUserBtn" type="button" value="deleteUser" />
                        <input id="resetUserApiKeyBtn" type="button" value="resetApiKey" />
                    </div>
                    <div style="margin-top: 10px;">
                        Response: <span id="status_users_response"></span><br/>
                        <textarea id="users_response" style="height: 60px;"></textarea>
                    </div>
                </div>
            </div>

            <!-- Clients tab -->
            <div class="menu_content menu_clients">
                <h2>Clients</h2>

                <div>
                    CustomerNumber:
                    <input id="clients_customerNumber" class="textinput" value="rcn-000-000-000"/>
                </div>
                <div style="margin-top: 10px;">
                    <input id="getClientsBtn" type="button" value="getClients" />
                    <div style="margin-top: 10px;">
                        Response: <span id="status_get_clients"></span><br/>
                        <textarea id="get_clients_response" style="height: 160px;"></textarea>
                    </div>
                </div>

                <div style="margin-top: 10px;">
                    Client Name: <input id="clients_clientName" value="test app" />
                    <input id="registerClientBtn" type="button" value="registerClient" />
                    <div style="margin-top: 5px;">
                        Response: <span id="status_register_client"></span><br/>
                        <textarea id="register_client_response" style="height: 60px;"></textarea>
                    </div>
                </div>

                <div style="margin-top: 10px;">
                    <table cellspacing="3">
                    <tr>
                        <td>ClientId:</td>
                        <td><input id="clients_clientId" value="ABCDEF" /></td>
                    </tr>
                    </table>
                    <div style="margin-top: 5px;">
                        <input id="getClientBtn" type="button" value="getClient" />
                        <input id="deleteClientBtn" type="button" value="deleteClient" onclick="alert('Not Implemented.');" disabled/>
                        <input id="getClientPermissionsBtn" type="button" value="getClientPermissions" />
                    </div>
                    <div style="margin-top: 5px;">
                        Response: <span id="status_clients_response"></span><br/>
                        <textarea id="clients_response" style="height: 60px;"></textarea>
                    </div>
                </div>
            </div>

            <!-- Customers tab -->
            <div class="menu_content menu_customers">
                <h2>Customers</h2>
                <div>
                    CustomerNumber: <input id="customerNumber" class="textinput" value="RCN-000-000-000" />
                </div>
                <div style="margin-top: 10px;">
                    <input id="lockCustomerBtn" type="button" value="lockCustomer" />&nbsp;
                    <input id="unlockCustomerBtn" type="button" value="unlockCustomer" />
                </div>
                <div style="margin-top: 10px;">
                    Response: <span id="status_customer_response"></span><br/>
                    <textarea id="customer_response" style="width: 300px; height: 100px;"></textarea>
                </div>
            </div>

            <!-- Password tab -->
            <div class="menu_content menu_password">
                <h2>Password</h2>

                <table cellspacing="3">
                <tr>
                    <td>Password:</td>
                    <td><input id="password_password" value="P@ssw0rd!" style="width: 300px;" /></td>
                </tr>
                </table>

                <div style="margin-top: 10px;">
                    <input id="getPasswordRulesBtn" type="button" value="getPasswordRules" />
                    <input id="validatePasswordBtn" type="button" value="validatePassword" />
                </div>
                <div style="margin-top: 5px;">
                    Response: <span id="status_password_response"></span><br/>
                    <textarea id="password_response" style="height: 100px;"></textarea>
                </div>

                <table cellspacing="3" style="margin-top: 10px;">
                <tr>
                    <td>CustomerNumber:</td>
                    <td><input id="password_customerNumber" value="rcn-000-000-000" style="width: 250px;" /></td>
                </tr>
                <tr>
                    <td>Username:</td>
                    <td><input id="password_username" value="huey"/></td>
                </tr>
                <tr>
                    <td>Password:</td>
                    <td><input id="password_userpassword" value="P@ssw0rd"/></td>
                </tr>
                <tr>
                    <td>NewPassword:</td>
                    <td><input id="password_usernewpassword" value="P@ssw0rd"/></td>
                </tr>
                </table>
                <div style="margin-top: 10px;">
                    <input id="getPasswordBtn" type="button" value="getUserPassword" />
                    <input id="setPasswordBtn" type="button" value="setUserPassword" />
                    <input id="resetPasswordBtn" type="button" value="resetUserPassword" />
                </div>
                <div style="margin-top: 5px;">
                    Response: <span id="status_userpassword"></span><br/>
                    <textarea id="password_userpassword_response" style="height: 100px;"></textarea>
                </div>
            </div>

            <!-- Tokens tab -->
            <div class="menu_content menu_tokens">
                <h2>Tokens</h2>
                <div>
                    <input id="tokens_tokenId" class="textinput" value="XXXXXX" style="width: 310px" />
                    <br/>
                    <div style="margin-top: 10px;">
                        <input id="validateTokenBtn" type="button" value="validateToken" />
                        <input id="revokeTokenBtn" type="button" value="revokeToken" />
                    </div>
                </div>
                <div style="margin-top: 10px;">
                    Response: <span id="status_tokens_response"></span><br/>
                    <textarea id="tokens_response" style="height: 100px;"></textarea>
                </div>
            </div>
    </div>
</div>
<div class="clearthis">&nbsp;</div>

</div></div>
</body>
</html>