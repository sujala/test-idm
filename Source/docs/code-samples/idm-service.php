<?php
define("IDM_API_URL", "http://localhost:8080/idm");
define("IDM_NAMESPACE", "http://docs.rackspacecloud.com/idm/api/v1.0");

class idmService {
    protected $_url = IDM_API_URL;
    protected $_apiNamespace = IDM_NAMESPACE;
    protected $_accessToken;

    public function __construct($accessToken = "")
    {
        $this->apiNamespace = $apiNamespace;
        $this->_accessToken = $accessToken;
    }

    /**
     * Get an access token
     *
     * @param string $clientId
     * @param string $clientSecret
     * @param string $username
     * @param string $password
     *
     * @return string
     */
    public function getToken($tokenType, $clientId, $clientSecret, 
        $username, $password, $refreshToken){

        $grantType = "NONE";

        switch($tokenType) {
            case "admin":
            case "non-admin":
            case "racker":
                $grantType = "PASSWORD";
                break;
            case "refresh-token":
                $grantType = "REFRESH_TOKEN";
                break;
            default:
                $grantType = "NONE";
        }

        $data = "<authCredentials " .
                    "xmlns=\"$this->_apiNamespace\" " .
                    "client_id=\"$clientId\" " .
                    "client_secret=\"$clientSecret\" " .
                    "username=\"$username\" " .
                    "password=\"$password\" " .
                    "refresh_token=\"$refreshToken\" " .
                    "grant_type=\"$grantType\" />";

        $url = $this->_url . "/token";

        $result = $this->_sendPost($url, $data);

        return $result;
    }

    /**
     * Add a user
     *
     * @param string $customerNumber
     * @param string $username
     * @param string $password
     * @param string $firstName
     * @param string $lastName
     * @param string $middleName
     * @param string $email
     * @param string secretQuestion
     * @param string secretAnswer
     *
     * @return string
     */
    public function addUser($customerNumber, $username, $password,
        $firstName, $lastName, $middleName, $email,
        $secretQuestion, $secretAnswer) {

        $data =
            "<user xmlns=\"$this->_apiNamespace\" " .
                "softDeleted=\"false\" locked=\"false\" status=\"ACTIVE\" region=\"America/Chicago\" " .
                "prefLanguage=\"US_en\" displayName=\"$firstName $lastName\" lastName=\"$lastName\" " .
                "middleName=\"$middleName\" firstName=\"$firstName\" email=\"$email\" " .
                "customerId=\"$customerNumber\" " .
                "username=\"$username\"> " .
                "<secret secretAnswer=\"$secretAnswer\" " .
                    "secretQuestion=\"$secretQuestion\" />" .
                "<password password=\"$password\" />" .
            "</user>";

        $url = $this->_url . "/customers/$customerNumber/users";
        $result = $this->_sendPost($url, $data);

        return $result;
    }

    /**
     * Update a user
     *
     * @param string $customerNumber
     * @param string $username
     * @param string $firstName
     * @param string $lastName
     * @param string $middleName
     * @param string $email
     *
     * @return string
     */
    public function updateUser($customerNumber, $username,
        $firstName, $lastName, $middleName, $email,
        $secretQuestion, $secretAnswer) {

        $data =
            "<user xmlns=\"$this->_apiNamespace\" " .
                "softDeleted=\"false\" locked=\"false\" status=\"ACTIVE\" region=\"America/Chicago\" " .
                "prefLanguage=\"US_en\" displayName=\"$firstName $lastName\" lastName=\"$lastName\" " .
                "middleName=\"$middleName\" firstName=\"$firstName\" email=\"$email\" " .
                "customerId=\"$customerNumber\" " .
                "username=\"$username\"> " .
                "<secret secretAnswer=\"$secretAnswer\" " .
                    "secretQuestion=\"$secretQuestion\" />" .
            "</user>";

        $url = $this->_url . "/customers/$customerNumber/users/$username";
        $result = $this->_sendPut($url, $data);

        return $result;
    }

    /**
     * Get a user
     *
     * @param string $customerNumber
     * @param string $username
     *
     * @return string
     */
    public function getUser($customerNumber, $username) {

        $url = $this->_url . "/customers/$customerNumber/users/$username";
        $result = $this->_sendGet($url);

        return $result;
    }

    /**
     * Delete a user
     *
     * @param string $customerNumber
     * @param string $username
     *
     * @return string
     */
    public function deleteUser($customerNumber, $username) {

        $url = $this->_url . "/customers/$customerNumber/users/$username";
        $result = $this->_sendDelete($url);

        return $result;
    }

    /* -------------------- Private Funcs -----------------------*/

    /**
     * Send Post request
     *
     * @param string $url
     * @param string $data
     * @param array() $options
     *
     * @return string
     */
    private function _sendPost($url, $data, array $options = array()) {

        $defaults = array(
            CURLOPT_POST => 1,
            CURLOPT_HEADER => 0,
            CURLOPT_URL => $url,
            CURLOPT_FRESH_CONNECT => 1,
            CURLOPT_RETURNTRANSFER => 1,
            CURLOPT_FORBID_REUSE => 1,
            CURLOPT_TIMEOUT => 4,
            CURLOPT_POSTFIELDS => $data
        );

        $ch = curl_init();
        curl_setopt_array($ch, ($options + $defaults));

        $headers = array(
            "Content-Type: application/xml",
            "Accept: application/xml"
        );
        if ($this->_accessToken != "") {
            array_push($headers, "Authorization: OAuth $this->_accessToken");
        }
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        if( ! $result = curl_exec($ch))
        {
            trigger_error(curl_error($ch));
        }

        $info = curl_getinfo($ch);
        $statusCode = $info['http_code'];

        $retval = array(
            'statusCode'=>$statusCode,
            'result'=> urlencode($result)
        );

        curl_close($ch);

        return $retval;
    }

    /**
     * Send Put request
     *
     * @param string $url
     * @param string $data
     * @param array() $options
     *
     * @return string
     */
    private function _sendPut($url, $data, array $options = array()) {

        $defaults = array(
            CURLOPT_HEADER => 0,
            CURLOPT_URL => $url,
            CURLOPT_FRESH_CONNECT => 1,
            CURLOPT_RETURNTRANSFER => 1,
            CURLOPT_FORBID_REUSE => 1,
            CURLOPT_TIMEOUT => 4,
            CURLOPT_POSTFIELDS => $data
        );

        $ch = curl_init();
        curl_setopt_array($ch, ($options + $defaults));
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');

        $headers = array(
            "Content-Type: application/xml",
            "Accept: application/xml"
        );
        if ($this->_accessToken != "") {
            array_push($headers, "Authorization: OAuth $this->_accessToken");
        }
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        if( !$result = curl_exec($ch))
        {
            trigger_error(curl_error($ch));
        }

        $info = curl_getinfo($ch);
        $statusCode = $info['http_code'];

        $retval = array(
            'statusCode'=>$statusCode,
            'result'=> urlencode($result)
        );

        curl_close($ch);

        return $retval;
    }

    /**
     * Send Get request
     *
     * @param string $url
     * @param array() $options
     *
     * @return string
     */
    private function _sendGet($url, array $options = array()) {

        $ch=curl_init();

        $defaults = array(
            CURLOPT_HEADER => 0,
            CURLOPT_URL => $url,
            CURLOPT_FRESH_CONNECT => 1,
            CURLOPT_RETURNTRANSFER => 1,
            CURLOPT_FORBID_REUSE => 1,
            CURLOPT_TIMEOUT => 4
        );

        curl_setopt_array($ch, ($options + $defaults));
        $headers = array("Accept: application/xml");
        if ($this->_accessToken != "") {
            array_push($headers, "Authorization: OAuth $this->_accessToken");
        }
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        if( !$result = curl_exec($ch))
        {
            trigger_error(curl_error($ch));
        }

        $info = curl_getinfo($ch);
        $statusCode = $info['http_code'];
        
        $retval = array(
            'statusCode'=>$statusCode,
            'result'=> urlencode($result)
        );
        curl_close($ch);

        return $retval;
    }

    /**
     * Send Delete request
     *
     * @param string $url
     * @param array() $options
     *
     * @return string
     */
    private function _sendDelete($url, array $options = array()) {

        $ch=curl_init();

        $defaults = array(
            CURLOPT_HEADER => 0,
            CURLOPT_URL => $url,
            CURLOPT_FRESH_CONNECT => 1,
            CURLOPT_RETURNTRANSFER => 1,
            CURLOPT_FORBID_REUSE => 1,
            CURLOPT_TIMEOUT => 4
        );

        curl_setopt_array($ch, ($options + $defaults));
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "DELETE");
        $headers = array("Accept: application/xml");
        if ($this->_accessToken != "") {
            array_push($headers, "Authorization: OAuth $this->_accessToken");
        }
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        $result = curl_exec($ch);

        $info = curl_getinfo($ch);
        $statusCode = $info['http_code'];

        $retval = array(
            'statusCode'=>$statusCode,
            'result'=> urlencode($result)
        );

        curl_close($ch);

        return $retval;
    }
}
?>