package com.rackspace.idm.domain.dao.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApiDocDao;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.TimeUnit;

@Component
public class FileSystemApiDocRepository implements ApiDocDao {
    public static final int BUFFER_SZ = 1024;

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemApiDocRepository.class);

    @Autowired
    IdentityConfig identityConfig;

    /**
     * Use a reloadable cache for this so don't have to reload from the filesystem every time.
     */
    private volatile LoadingCache<String, String> versionInfo = null;

    public FileSystemApiDocRepository() {
    }

    public FileSystemApiDocRepository(IdentityConfig config) {
        identityConfig = config;
    }

    public String getContent(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }

        String result;

        /*
        search within config folder for the file. Fallback to original loading from classpath if not found or encounter any error.
         */
        try {
            result = getLazyInitVersionInfo().getUnchecked(path);
        } catch (Exception e) {
            LOG.info(String.format("Error loading '%s' from reloadable docs. Falling back to classpath.", path), e);
            result = loadFromClassPath(path);
        }

        return result;
    }

    /**
     * This performs lazy initialization of the version cache. This allows it to only be initialized when actually used.
     *
     * @return
     */
    private LoadingCache<String, String> getLazyInitVersionInfo() {
        LoadingCache<String, String> result = versionInfo;
        if (result == null) {
            synchronized (this) {
                result = versionInfo;
                if (result == null) {
                    versionInfo = result = initVersionInfo();
                }
            }
        }
        return result;
    }

    private LoadingCache<String, String> initVersionInfo() {
        LoadingCache<String, String> versionCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(identityConfig.getStaticConfig().reloadableDocsTimeOutInSeconds(), TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, String>() {
                            /**
                             * On cache miss or expiration, this method will be called to retrieve the content and cache
                             * it for future use.
                             *
                             * @param path
                             * @return
                             */
                            public String load(String path) {
                                return getCacheableContent(path);
                            }
                        });

        return versionCache;
    }

    private String getCacheableContent(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }

        /*
        search within config folder for the file. Fallback to original loading from classpath if not found or encounter any error.
         */
        String result = "";
        String configLocation = identityConfig.getConfigRoot();
        if (StringUtils.isNotBlank(configLocation)) {
            File fileSystemPath = new File(configLocation, path);
            FileSystemResource fileSystemResource = new FileSystemResource(fileSystemPath);
            if (fileSystemResource.exists()) {
                InputStream contentStream = null;
                try {
                    contentStream = fileSystemResource.getInputStream();
                    result = convertStreamToString(contentStream);
                } catch (IOException e) {
                    //eat and load from classpath
                } finally {
                    IOUtils.closeQuietly(contentStream);
                }
            } else {
                LOG.info(String.format("External resources not found at '%s'. Loading default", fileSystemPath.getPath()));
            }
        }

        if (StringUtils.isBlank(result)) {
            result = loadFromClassPath(path);
        }

        return result;
    }

    private String loadFromClassPath(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        String result = "";

        if (resource.exists()) {
            InputStream stream = null;
            try {
                stream = resource.getInputStream();
                if (stream != null) {
                    result = convertStreamToString(stream);
                }
            } catch (IOException e) {
                //eat. Just return empty string
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return result;
    }

    String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[BUFFER_SZ];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

}
