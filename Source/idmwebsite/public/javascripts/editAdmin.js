// Globals

var firstnamevalid = false;
var lastnamevalid = false;
var emailvalid = false;
var secretquestionvalid = false;
var secretanswervalid = false;

// functions
function toggleRequirements(elem)
{
    var effect = 'slide';
    var options = {};
    var effectmms = 500;

    // hide all requirements
    $("#firstname_requirements").hide();
    $("#lastname_requirements").hide();
    $("#middlename_requirements").hide();
    $("#email_requirements").hide();
    $("#secretquestion_requirements").hide();
    $("#secretanswer_requirements").hide();

    switch(elem.name)
    {
        case "firstname":
            $("#firstname_requirements").toggle(effect, options, effectmms); break;
        case "lastname":
            $("#lastname_requirements").toggle(effect, options, effectmms); break;
        case "middlename":
            $("#middlename_requirements").toggle(effect, options, effectmms); break;
        case "email":
            $("#email_requirements").toggle(effect, options, effectmms); break;
        case "secretquestion":
            $("#secretquestion_requirements").toggle(effect, options, effectmms); break;
        case "secretanswer":
            $("#secretanswer_requirements").toggle(effect, options, effectmms); break;
    }
}

function validateFirstname()
{
    var firstname = document.editadminform.firstname.value;

    firstnamevalid = true;
    if (firstname.length < 1)
    {
        setRule('req_firstnamelength', 'failed');
        firstnamevalid = false;
    }
    else
    {
        setRule('req_firstnamelength', 'passed');
    }
    toggleIcon('firstname', firstnamevalid);
}

function validateLastname()
{
    var lastname = document.editadminform.lastname.value;

    lastnamevalid = true;
    if (lastname.length < 1)
    {
        setRule('req_lastnamelength', 'failed');
        lastnamevalid = false;
    }
    else
    {
        setRule('req_lastnamelength', 'passed');
    }
    toggleIcon('lastname', lastnamevalid);
}

function validateEmail()
{
    var email = document.editadminform.email.value;

    var emailRegex = new RegExp( /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/);
    if (emailRegex.test(email)) {
        setRule('req_emailvalid', 'passed');
        emailvalid = true;
    }
    else {
        setRule('req_emailvalid', 'failed');
        emailvalid = false;
    }

    toggleIcon('email', emailvalid);
}

function validateSecretQuestion()
{
    var secretquestion = document.editadminform.secretquestion.value;

    secretquestionvalid = true;
    if (secretquestion.length < 1)
    {
        setRule('req_secretquestionlength', 'failed');
        secretquestionvalid = false;
    }
    else
    {
        setRule('req_secretquestionlength', 'passed');
    }
    toggleIcon('secretquestion', secretquestionvalid);
}

function validateSecretAnswer()
{
    var secretanswer = document.editadminform.secretanswer.value;

    secretanswervalid = true;
    if (secretanswer.length < 1)
    {
        setRule('req_secretanswerlength', 'failed');
        secretanswervalid = false;
    }
    else
    {
        setRule('req_secretanswerlength', 'passed');
    }
    toggleIcon('secretanswer', secretanswervalid);
}

function loadXmlDoc(text)
{
    if (window.DOMParser)
    {
        parser=new DOMParser();
        xmlDoc=parser.parseFromString(text,"text/xml");
    }
    else // Internet Explorer
    {
        xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
        xmlDoc.async="false";
        xmlDoc.loadXML(text);
    }
    return xmlDoc;
}

function getNodeVal(xmldoc, nodename)
{
    if (xmldoc.getElementsByTagName(nodename)[0]){
        return xmldoc.getElementsByTagName(nodename)[0].firstChild.nodeValue;
    }
    else {
        return 0;
    }
}

function setRule(id, passfail)
{
    if (passfail == 'passed')
    {
        $('#' +id).attr("class","requirement_bullet_green");
    }
    else
    {
        $('#' +id).attr("class","requirement_bullet_gray");
    }
}

function strrev(str) {
  return str.split("").reverse().join("");
}

function toggleIcon(fieldName, fieldValid)
{
    if (fieldValid)
    {
        $('#' + fieldName + '_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#' + fieldName + '_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function toggleNextStep()
{
    if (allFieldsValid())
    {
        $('#btn_nextstep').fadeTo('fast', 1.0);
    }
    else
    {
        $('#btn_nextstep').fadeTo('fast', 0.2);
    }
}

function allFieldsValid()
{
    if (firstnamevalid &&
        lastnamevalid &&
        emailvalid &&
        secretquestionvalid &&
        secretanswervalid)
    {
        return true;
    }
    return false;
}

function submitForm()
{
    if (allFieldsValid())
    {
        document.editadminform.submit();
    }
}