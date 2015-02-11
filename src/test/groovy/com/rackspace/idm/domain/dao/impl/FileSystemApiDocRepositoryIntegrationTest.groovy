package com.rackspace.idm.domain.dao.impl

import com.google.common.cache.LoadingCache
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Integration test in the sense that it includes testing limited interaction with the loading cache. The rest of the
 * app is not included in this test.
 */
class FileSystemApiDocRepositoryIntegrationTest extends RootServiceTest {

    FileSystemApiDocRepository fileSystemApiDocRepository = new FileSystemApiDocRepository();

    @Shared File tempDir

    def setupSpec() {
        def originalResourceContent = "test"
        File tempFile = File.createTempFile("temp", ".resource")
        tempDir = tempFile.getParentFile()
        tempFile.deleteOnExit()
    }

    def setup() {
        mockIdentityConfig(fileSystemApiDocRepository)

        def originalResourceContent = "test"
        File tempFile = File.createTempFile("temp", ".resource")
        tempFile.deleteOnExit()
        tempFile.withWriter { out ->
            out.write(originalResourceContent)
        }

    }

    def "getContent service uses props to determine whether to use cache and to initialize it"() {
        given:
        def path = "path"

        when:
        fileSystemApiDocRepository.getContent(path)

        then:
        1 * identityConfig.useReloadableDocs() >> false
        fileSystemApiDocRepository.versionInfo == null

        when:
        fileSystemApiDocRepository.getContent(path)

        then:
        1 * identityConfig.useReloadableDocs() >> true
        1 * identityConfig.reloadableDocsTimeOutInSeconds() >> 100
        fileSystemApiDocRepository.versionInfo != null
    }

    def "errors loaded from cache are eaten"() {
        given:
        def path = "path"
        LoadingCache mockCache = Mock(LoadingCache)
        fileSystemApiDocRepository.versionInfo = mockCache

        when:
        fileSystemApiDocRepository.getContent(path)

        then:
        1 * identityConfig.useReloadableDocs() >> true
        1 * mockCache.getUnchecked(path) >> {throw new RuntimeException("Random Exception")}
        notThrown(RuntimeException)
    }

    def "missing content returns empty string"() {
        given:
        File tempFile = File.createTempFile("temp", ".resource")
        tempFile.deleteOnExit()
        def path = UUID.randomUUID().toString()

        when: "using reloadable"
        String returnedContent = fileSystemApiDocRepository.getContent(path);

        then:
        1 * identityConfig.useReloadableDocs() >> true
        1 * identityConfig.getConfigRoot() >> tempFile.getParentFile().getAbsolutePath()
        returnedContent == ""

        when: "using fallback"
        returnedContent = fileSystemApiDocRepository.getContent(path);

        then:
        1 * identityConfig.useReloadableDocs() >> false
        returnedContent == ""
    }

    def "files loaded from config root"() {
        given:
        def resourceContent = "test"
        File tempFile = File.createTempFile("temp", ".resource")
        tempFile.deleteOnExit()
        tempFile.withWriter { out ->
                out.write(resourceContent)
        }

        def path = tempFile.name

        when:
        String returnedContent = fileSystemApiDocRepository.getContent(path)

        then:
        1 * identityConfig.useReloadableDocs() >> true
        1 * identityConfig.getConfigRoot() >> tempFile.getParentFile().getAbsolutePath()
        returnedContent == resourceContent
    }

    def "cache expiration causes reload"() {
        given:
        def resourceContent = "test"
        File tempFile = File.createTempFile("temp", ".resource")
        tempFile.deleteOnExit()
        tempFile.withWriter { out ->
            out.write(resourceContent)
        }

        def path = tempFile.name

        identityConfig.getConfigRoot() >> tempFile.getParentFile().getAbsolutePath()
        identityConfig.reloadableDocsTimeOutInSeconds() >> 0
        identityConfig.useReloadableDocs() >> true

        when:
        String returnedContent = fileSystemApiDocRepository.getContent(path)

        then:
        returnedContent == resourceContent

        when: "change content"
        def newResourceContent = "content changed"
        tempFile.withWriter { out ->
            out.write(newResourceContent)
        }
        returnedContent = fileSystemApiDocRepository.getContent(path)

        then:
        returnedContent == newResourceContent
    }

    def "cache returns same content when underlying content changes"() {
        given:
        def originalResourceContent = "test"
        File tempFile = File.createTempFile("temp", ".resource")
        tempFile.deleteOnExit()
        tempFile.withWriter { out ->
            out.write(originalResourceContent)
        }

        def path = tempFile.name

        identityConfig.getConfigRoot() >> tempFile.getParentFile().getAbsolutePath()
        identityConfig.reloadableDocsTimeOutInSeconds() >> 1000
        identityConfig.useReloadableDocs() >> true

        String returnedContent = fileSystemApiDocRepository.getContent(path)
        assert returnedContent == originalResourceContent
        def newResourceContent = "content changed"
        tempFile.withWriter { out ->
            out.write(newResourceContent)
        }

        when: "get content after undelying content has changed"
        returnedContent = fileSystemApiDocRepository.getContent(path)

        then: "old content still returned"
        returnedContent == originalResourceContent
    }

    def "getting content falls back to classpath when not found"() {
        given:
        def path = "app-config.xml" //a file that must exist at root of classpath, but not in temp directory
        File testFile = new File(tempDir, path)
        assert !testFile.exists() //make sure files does not exist in temp directory or test is invalid

        identityConfig.getConfigRoot() >> tempDir.getAbsolutePath()

        when: "using reloadable"
        String returnedContent = fileSystemApiDocRepository.getContent(path);

        then:
        1 * identityConfig.useReloadableDocs() >> true
        returnedContent != ""

        when: "using fallback"
        returnedContent = fileSystemApiDocRepository.getContent(path);

        then:
        1 * identityConfig.useReloadableDocs() >> false
        returnedContent != ""
    }


}
