/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2017 The ZAP Development Team
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
package org.zaproxy.zap.spider.filters;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.spider.DomainAlwaysInScopeMatcher;
import org.zaproxy.zap.spider.filters.FetchFilter.FetchStatus;

/**
 * Unit test for {@link DefaultFetchFilter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultFetchFilterUnitTest {

    @Mock
    Context context;

    private DefaultFetchFilter filter;

    @BeforeClass
    public static void suppressLogging() {
        Logger.getRootLogger().addAppender(new NullAppender());
    }

    @Before
    public void setUp() {
        filter = new DefaultFetchFilter();
    }

    @Test
    public void shouldFilterUriWithNonSchemeAsIllegalProtocol() throws Exception {
        // Given
        URI uri = createUri("example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.ILLEGAL_PROTOCOL)));
    }

    @Test
    public void shouldFilterUriWithNonHttpOrHttpsSchemeAsIllegalProtocol() throws Exception {
        // Given
        URI uri = createUri("ftp://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.ILLEGAL_PROTOCOL)));
    }

    @Test
    public void shouldFilterUriWithHttpSchemeAsOutOfScopeByDefault() throws Exception {
        // Given
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.OUT_OF_SCOPE)));
    }

    @Test
    public void shouldFilterUriWithHttpsSchemeAsOutOfScopeByDefault() throws Exception {
        // Given
        URI uri = createUri("https://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.OUT_OF_SCOPE)));
    }

    @Test
    public void shouldFilterOutOfScopeUriAsOutOfScope() throws Exception {
        // Given
        filter.addScopeRegex("scope.example.com");
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.OUT_OF_SCOPE)));
    }

    @Test
    public void shouldFilterInScopeUriAsValid() throws Exception {
        // Given
        filter.addScopeRegex("example.com");
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.VALID)));
    }

    @Test
    public void shouldFilterNonAlwaysInScopeUriAsOutOfScope() throws Exception {
        // Given
        filter.setDomainsAlwaysInScope(domainsAlwaysInScope("scope.example.com"));
        URI uri = createUri("https://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.OUT_OF_SCOPE)));
    }

    @Test
    public void shouldFilterAlwaysInScopeUriAsValid() throws Exception {
        // Given
        filter.setDomainsAlwaysInScope(domainsAlwaysInScope("example.com"));
        URI uri = createUri("https://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.VALID)));
    }

    @Test
    public void shouldFilterExcludedInScopeUriAsUserRules() throws Exception {
        // Given
        filter.addScopeRegex("example.com");
        filter.setExcludeRegexes(excludeRegexes(".*example\\.com.*"));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.USER_RULES)));
    }

    @Test
    public void shouldFilterExcludedAlwaysInScopeUriAsUserRules() throws Exception {
        // Given
        filter.setDomainsAlwaysInScope(domainsAlwaysInScope("example.com"));
        filter.setExcludeRegexes(excludeRegexes(".*example\\.com.*"));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.USER_RULES)));
    }

    @Test
    public void shouldFilterNonExcludedInScopeUriAsValid() throws Exception {
        // Given
        filter.addScopeRegex("example.com");
        filter.setExcludeRegexes(excludeRegexes("subdomain\\.example\\.com.*"));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.VALID)));
    }

    @Test
    public void shouldFilterNonExcludedAlwaysInScopeUriAsValid() throws Exception {
        // Given
        filter.setDomainsAlwaysInScope(domainsAlwaysInScope("example.com"));
        filter.setExcludeRegexes(excludeRegexes("subdomain\\.example\\.com.*"));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.VALID)));
    }

    @Test
    public void shouldFilterOutOfContextUriAsOutOfContext() throws Exception {
        // Given
        filter.setScanContext(contextInScope(false));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.OUT_OF_CONTEXT)));
    }

    @Test
    public void shouldFilterInContextUriAsValid() throws Exception {
        // Given
        filter.setScanContext(contextInScope(true));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.VALID)));
    }

    @Test
    public void shouldFilterExcludedInContextUriAsUserRules() throws Exception {
        // Given
        filter.setScanContext(contextInScope(true));
        filter.setExcludeRegexes(excludeRegexes(".*example\\.com.*"));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.USER_RULES)));
    }

    @Test
    public void shouldFilterNonExcludedInContextUriAsValid() throws Exception {
        // Given
        filter.setScanContext(contextInScope(true));
        filter.setExcludeRegexes(excludeRegexes("subdomain\\.example\\.com.*"));
        URI uri = createUri("http://example.com");
        // When
        FetchStatus status = filter.checkFilter(uri);
        // Then
        assertThat(status, is(equalTo(FetchStatus.VALID)));
    }

    private static URI createUri(String uri) {
        try {
            return new URI(uri, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<DomainAlwaysInScopeMatcher> domainsAlwaysInScope(String... domains) {
        if (domains == null || domains.length == 0) {
            return Collections.emptyList();
        }

        List<DomainAlwaysInScopeMatcher> domainsAlwaysInScope = new ArrayList<>(1);
        for (String domain : domains) {
            domainsAlwaysInScope.add(new DomainAlwaysInScopeMatcher(domain));
        }
        return domainsAlwaysInScope;
    }

    private static List<String> excludeRegexes(String... regexes) {
        if (regexes == null || regexes.length == 0) {
            return Collections.emptyList();
        }

        List<String> excludedRegexes = new ArrayList<>(1);
        for (String regex : regexes) {
            excludedRegexes.add(regex);
        }
        return excludedRegexes;
    }

    private Context contextInScope(boolean inScope) {
        given(context.isInContext(anyString())).willReturn(inScope);
        return context;
    }
}
