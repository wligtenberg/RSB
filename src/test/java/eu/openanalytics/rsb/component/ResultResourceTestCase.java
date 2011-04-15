/*
 *   R Service Bus
 *   
 *   Copyright (c) Copyright of OpenAnalytics BVBA, 2010-2011
 *
 *   ===========================================================================
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.openanalytics.rsb.component;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;

import org.apache.activemq.util.ByteArrayOutputStream;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.util.FileCopyUtils;

import eu.openanalytics.rsb.config.Configuration;

/**
 * @author "Open Analytics <rsb.development@openanalytics.eu>"
 */
@RunWith(MockitoJUnitRunner.class)
public class ResultResourceTestCase {
    private static final String MISSING_RSB_RESULT = "_missing_rsb_result_";
    private ResultResource resultResource;
    private File tempDir;
    private String testApplicationName;
    private String testResult;
    private String testResultPayload;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Before
    public void prepareTest() {
        tempDir = new File(System.getProperty("java.io.tmpdir"));
        testApplicationName = tempDir.getName();
        testResult = "rsb-" + UUID.randomUUID().toString() + ".tst";
        testResultPayload = RandomStringUtils.randomAlphanumeric(25 + RandomUtils.nextInt(25));

        final Configuration configuration = mock(Configuration.class);
        when(configuration.getRsbResultsDirectory()).thenReturn(tempDir.getParentFile());

        resultResource = new ResultResource();
        resultResource.setConfiguration(configuration);
    }

    @Test(expected = WebApplicationException.class)
    public void getResultNotFound() throws IOException {
        resultResource.getResult(testApplicationName, MISSING_RSB_RESULT, httpServletResponse);
    }

    @Test
    public void getResult() throws IOException {
        createTestResultFile();

        final StreamingOutput result = resultResource.getResult(testApplicationName, testResult, httpServletResponse);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.write(baos);

        assertThat(baos.toByteArray(), is(testResultPayload.getBytes()));

        verify(httpServletResponse).addHeader(HttpHeaders.ETAG, ResultResource.getEtag(testApplicationName, testResult));
    }

    @Test(expected = WebApplicationException.class)
    public void getResultMetaNotFound() {
        resultResource.getResultMeta(testApplicationName, MISSING_RSB_RESULT, httpServletResponse);
    }

    @Test
    public void getResultMeta() throws IOException {
        final File testResultFile = createTestResultFile();

        resultResource.getResultMeta(testApplicationName, testResult, httpServletResponse);

        verify(httpServletResponse).addHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(testResultFile.length()));
        verify(httpServletResponse).addHeader(HttpHeaders.ETAG, ResultResource.getEtag(testApplicationName, testResult));
    }

    private File createTestResultFile() throws IOException {
        final File testResultFile = new File(tempDir, testResult);
        testResultFile.deleteOnExit();
        FileCopyUtils.copy(testResultPayload, new FileWriter(testResultFile));
        return testResultFile;
    }
}
