package com.rackspace.idm.multifactor.providers.duo.service;

import com.google.common.base.Charsets;
import com.rackspace.idm.multifactor.providers.duo.config.DuoSecurityConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A helper class to abstract consuming Duo Security REST services. A DuoSecurityConfig object must be provided which specifies the various secrets that must be provided in order
 * to sign the payload as required by Duo Security to accept the request.
 */
public class DefaultDuoRequestHelper implements DuoRequestHelper {
    private static final String DUO_HMAC_ALOGORITHM = "HmacSHA1";
    private DuoSecurityConfig config;
    private Client client;
    private URI baseURI;

    /**
     * A formatter to format dates in the Duo Security required format
     */
    public static DateTimeFormatter RFC822DATEFORMAT = DateTimeFormat.forPattern("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z").withLocale(Locale.US).withZone(DateTimeZone.UTC);

    /**
     * The newline style required by Duo Security when canonicalizing the request for signing in order to authenticate
     * with duo.
     */
    private static final String DUO_CANONICALIZATION_NEWLINE = "\n";

    public DefaultDuoRequestHelper(DuoSecurityConfig config) {
        this.config = config;
        client = Client.create();
        client.setConnectTimeout(config.getDefaultTimeout());

        //create the base URI that will be used as the base path for all requests
        try {
            URL url = new URL("https://" + config.getApiHostName());
            baseURI = url.toURI();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not create URL from api hostname '" + config.getApiHostName() + "'");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not create URI from api hostname '" + config.getApiHostName() + "'");
        }
    }

    /**
     * Creates a new web resource from the client for the configured api host name with an optional additional path.
     *
     * @param additionalPath
     * @return
     */
    @Override
    public WebResource createWebResource(String additionalPath) {
        if (StringUtils.isBlank(additionalPath)) {
            return client.resource(baseURI);
        }
        return client.resource(baseURI).path(additionalPath);
    }

    /**
     * Creates a delete request by calling {@link #buildDeleteWebResource(com.sun.jersey.api.client.WebResource, java.util.Map)} and then executes it.
     *
     * @param baseResource
     * @param params
     * @param responseClass
     * @param <T> the expected class that is returned
     * @return
     */
    @Override
    public <T> T makeDeleteRequest(final WebResource baseResource, final Map<String, String> params, Class<T> responseClass) {
        WebResource.Builder builder = buildDeleteWebResource(baseResource, params);
        return builder.delete(responseClass);
    }

    /**
     * Creates a DELETE WebResource with all appropriate signatures for calling against Duo Security
     *
     * @param baseResource
     * @param params
     * @return
     */
    @Override
    public WebResource.Builder buildDeleteWebResource(final WebResource baseResource, final Map<String, String> params) {
        WebResource newResource = baseResource.queryParams(createMultiValuedMap(params));
        WebResource.Builder builderResource = createAuthenticatedWebResourceForRequest(RequestMethod.DELETE, newResource, params);
        return builderResource;
    }

    /**
     * Creates a GET request by calling {@link #buildGetWebResource(com.sun.jersey.api.client.WebResource, java.util.Map)} and then executes it.
     *
     * @param baseResource
     * @param params
     * @param responseClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T makeGetRequest(final WebResource baseResource, final Map<String, String> params, Class<T> responseClass) {
        WebResource.Builder builder = buildGetWebResource(baseResource, params);
        return builder.get(responseClass);
    }

    /**
     * Creates a GET WebResource with all appropriate signatures for calling against Duo Security
     *
     * @param baseResource
     * @param params
     * @return
     */
    @Override
    public WebResource.Builder buildGetWebResource(final WebResource baseResource, final Map<String, String> params) {
        WebResource newResource = baseResource.queryParams(createMultiValuedMap(params));
        WebResource.Builder builderResource = createAuthenticatedWebResourceForRequest(RequestMethod.GET, newResource, params);
        return builderResource;
    }

    /**
     * Creates a POST WebResource with all appropriate signatures for calling against Duo Security
     *
     * @param baseResource
     * @param params
     * @return
     */
    @Override
    public WebResource.Builder buildPostWebResource(final WebResource baseResource, final Map<String, String> params) {
        //first get a _new_ resource with the query param. Technically, may not be necessary anymore since not using filters for authentication, but keeps each request isolated
        WebResource newResource = client.resource(baseResource.getURI());

        WebResource.Builder builderResource = createAuthenticatedWebResourceForRequest(RequestMethod.POST, newResource, params);
        builderResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        return builderResource;
    }

    /**
     * Creates a POST request by calling {@link #buildPostWebResource(com.sun.jersey.api.client.WebResource, java.util.Map)} and then executes it.
     *
     * @param baseResource
     * @param params
     * @param responseClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T makePostRequest(final WebResource baseResource, final Map<String, String> params, Class<T> responseClass) {
        WebResource.Builder builder = buildPostWebResource(baseResource, params);
        return builder.post(responseClass, createMultiValuedMap(params));
    }

    /**
     * Creates a WebResource builder with the appropriate security headings required by Duo Security
     *
     * @param requestMethod
     * @param baseResource
     * @return
     */
    private WebResource.Builder createAuthenticatedWebResourceForRequest(RequestMethod requestMethod, WebResource baseResource, Map<String, String> params) {
        Assert.notNull(requestMethod);
        Assert.notNull(baseResource);
        Assert.notNull(params);

        //Duo security requires params to be sorted lexographically in signature request
        TreeMap<String, String> sortedMap = new TreeMap<String, String>(params);

        String formattedParameters = urlEncodeParameters(sortedMap);
        DateTime now = new DateTime();
        String signature = sign(now, requestMethod, baseResource, formattedParameters);

        String authentication = createBasicAuthenticationHeader(signature);
        WebResource.Builder builder = baseResource.header(HttpHeaders.AUTHORIZATION, authentication).header("Date", getDuoCanonicallyFormattedDate(now)).accept(MediaType.APPLICATION_JSON);

        return builder;
    }

    private String createBasicAuthenticationHeader(String password) {
        try {
            return "Basic " + new String(Base64.encode(config.getIntegrationKey() + ":" + password), Charsets.US_ASCII.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding trying to base 64 encode signature", e);
        }
    }

    /**
     * Create and sign canonical string.
     *
     * @param dateTime            The date & time of the request. Must match the "Date" header of the request as far as
     *                            the instance in time.
     * @param requestMethod       HTTP request method being used
     * @param resource            the resource to which the request will be sent
     * @param formattedParameters an url encoded set of parameters that are lexographically ordered
     *                            return header line
     */
    private String sign(DateTime dateTime, RequestMethod requestMethod, WebResource resource, String formattedParameters) {
        String requestToSign = getCanonicalizedRequestToSign(dateTime, requestMethod, resource, formattedParameters);
        byte[] sign = generateHMac(config.getSecretKey(), requestToSign, DUO_HMAC_ALOGORITHM);
        String signature = new String(Hex.encodeHex(sign, true));
        return signature;
    }

    /**
     * Returns the Duo Security specified string that must be signed in order to authenticate
     *
     * @param dateTime            The date & time of the request. Must match the "Date" header of the request as far as
     *                            the instance in time.
     * @param requestMethod       The RequestMethod for the REST call
     * @param resource            The resource that will be called.
     * @param formattedParameters
     * @return
     */
    private String getCanonicalizedRequestToSign(DateTime dateTime, RequestMethod requestMethod, WebResource resource, String formattedParameters) {
        StringBuilder canon = new StringBuilder(getDuoCanonicallyFormattedDate(dateTime)).append(DUO_CANONICALIZATION_NEWLINE)
                .append(requestMethod.name().toUpperCase()).append(DUO_CANONICALIZATION_NEWLINE)
                .append(config.getApiHostName().toLowerCase()).append(DUO_CANONICALIZATION_NEWLINE)
                .append(resource.getURI().getPath()).append(DUO_CANONICALIZATION_NEWLINE)
                .append(formattedParameters);
        return canon.toString();
    }

    /**
     * The URL-encoded list of key=value pairs, lexicographically sorted by key.
     * These come from the request parameters (the URL query string for GET and DELETE requests or the request body for
     * POST requests).
     * Do not encode unreserved characters. Use upper-case hexadecimal digits A through F in escape sequences.
     *
     * @param params
     * @return
     */
    private String urlEncodeParameters(SortedMap<String, String> params) {
        Assert.notNull(params, "Must provide non-null map of parameters");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("Params can not contain null keys or values");
            }
        }

        StringBuilder signatureStringBuilder = new StringBuilder();
        if (!CollectionUtils.isEmpty(params)) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (signatureStringBuilder.length() > 0) {
                    signatureStringBuilder.append("&");
                }
                signatureStringBuilder.append((entry.getKey() != null ? encodeForDuoSecuritySignature(entry.getKey()) : ""));
                signatureStringBuilder.append("=");
                signatureStringBuilder.append(entry.getValue() != null ? encodeForDuoSecuritySignature(entry.getValue()) : "");
            }
        }
        return signatureStringBuilder.toString();
    }


    /**
     * Per Duo Security specifications @see <a href="https://www.duosecurity.com/docs/duoverify#request_format">Duo
     * Documentation</a>,
     * when URL-encoding, all bytes except ASCII letters, digits, underscore ("_"), period ("."), hyphen ("-"), and tilde ('~') are
     * replaced by a percent sign ("%") followed by two hexadecimal digits containing the value of the byte. For
     * example, a space is replaced with "%20".
     * Use only upper-case A through F for hexadecimal digits.
     */
    private String encodeForDuoSecuritySignature(String toEncode) {
        try {
            return URLEncoder.encode(toEncode, Charsets.UTF_8.name())
                    .replaceAll("\\*", "%2A") //duo requires encoded *
                    .replaceAll("%7E", "~")   //duo security doesn't like encoded '~', but encode will encode tildes, so we're reversing it here
                    .replaceAll("\\+", "%20"); //duo requires '%20' instead of '+' encoded spaces
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not encode parameters to required encoding format " + Charsets.UTF_8.name());
        }
    }

    private MultivaluedMap<String, String> createMultiValuedMap(Map<String, String> params) {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        if (!CollectionUtils.isEmpty(params)) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("Params can not contain null keys or values");
                }
                map.add(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    private String getDuoCanonicallyFormattedDate(DateTime dateTime) {
        return RFC822DATEFORMAT.print(dateTime);
    }

    /**
     * Create the HMAC of the data.
     *
     * @param secretKey
     * @param data
     * @param algorithm
     * @return
     */
    private byte[] generateHMac(String secretKey, String data, String algorithm /* e.g. "HmacSHA256" */) {
        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(), algorithm);
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
            return mac.doFinal(data.getBytes());
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid secret key provided.");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The system doesn't support algorithm " + algorithm, e);
        }
    }

}
