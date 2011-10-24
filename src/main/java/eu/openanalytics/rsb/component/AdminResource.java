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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import eu.openanalytics.rsb.Constants;
import eu.openanalytics.rsb.Util;
import eu.openanalytics.rsb.config.Configuration;
import eu.openanalytics.rsb.config.PersistedConfiguration;
import eu.openanalytics.rsb.cxf.ReloadableCXFServlet;
import eu.openanalytics.rsb.rest.types.Catalog;
import eu.openanalytics.rsb.rest.types.CatalogDirectory;
import eu.openanalytics.rsb.rest.types.CatalogFileType;

/**
 * @author "OpenAnalytics &lt;rsb.development@openanalytics.eu&gt;"
 */
@Component("adminResource")
@Path("/" + Constants.ADMIN_PATH)
public class AdminResource extends AbstractComponent implements ApplicationContextAware {

    private static final String CATALOG_SUBPATH = "catalog";
    private static final String SYSTEM_SUBPATH = "system";

    private ConfigurableApplicationContext applicationContext;

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Path("/" + SYSTEM_SUBPATH + "/configuration")
    @GET
    @Produces({ Constants.JSON_CONTENT_TYPE })
    public Response getSystemConfiguration() {
        return Response.ok(Util.toJson(new PersistedConfiguration(getConfiguration()))).build();
    }

    @Path("/" + SYSTEM_SUBPATH + "/restart")
    @POST
    @Produces({ Constants.TEXT_CONTENT_TYPE })
    public Response restart() {
        applicationContext.close();
        applicationContext.refresh();
        ReloadableCXFServlet.reloadAll();
        return Response.ok("RESTARTED").build();
    }

    @Path("/" + CATALOG_SUBPATH)
    @GET
    @Produces({ Constants.RSB_XML_CONTENT_TYPE, Constants.RSB_JSON_CONTENT_TYPE })
    public Catalog getCatalog(@Context final HttpHeaders httpHeaders, @Context final UriInfo uriInfo) throws IOException,
            URISyntaxException {

        final Catalog result = Util.REST_OBJECT_FACTORY.createCatalog();

        for (final Configuration.Catalog catalog : Configuration.Catalog.values()) {
            final CatalogDirectory catalogDirectory = createCatalogDirectory(catalog, httpHeaders, uriInfo);
            result.getDirectories().add(catalogDirectory);
        }

        return result;
    }

    @Path("/" + CATALOG_SUBPATH + "/{catalogName}/{fileName}")
    @GET
    public Response getCatalogFile(@PathParam("catalogName") final String catalogName, @PathParam("fileName") final String fileName,
            @Context final HttpHeaders httpHeaders, @Context final UriInfo uriInfo) throws IOException, URISyntaxException {

        final Configuration.Catalog catalog = Configuration.Catalog.valueOf(catalogName);
        final File catalogFile = new File(catalog.getConfiguredDirectory(getConfiguration()), fileName);

        if (!catalogFile.isFile()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        final ResponseBuilder rb = Response.ok();
        rb.type(Util.getContentType(catalogFile));
        rb.entity(new StreamingOutput() {
            public void write(final OutputStream output) throws IOException {
                final FileInputStream fis = new FileInputStream(catalogFile);
                IOUtils.copy(fis, output);
                IOUtils.closeQuietly(fis);
                IOUtils.closeQuietly(output);
            }
        });
        return rb.build();
    }

    @Path("/" + CATALOG_SUBPATH + "/{catalogName}/{fileName}")
    @PUT
    @Consumes(Constants.TEXT_CONTENT_TYPE)
    public Response putCatalogFile(@PathParam("catalogName") final String catalogName, @PathParam("fileName") final String fileName,
            final InputStream in, @Context final HttpHeaders httpHeaders, @Context final UriInfo uriInfo) throws IOException,
            URISyntaxException {

        final Configuration.Catalog catalog = Configuration.Catalog.valueOf(catalogName);
        final File catalogFile = new File(catalog.getConfiguredDirectory(getConfiguration()), fileName);
        final boolean preExistingFile = catalogFile.isFile();

        final FileWriter fw = new FileWriter(catalogFile);
        IOUtils.copy(in, fw);
        IOUtils.closeQuietly(fw);

        if (preExistingFile) {
            return Response.noContent().build();
        }

        final URI location = buildCatalogFileUri(catalog, catalogFile, httpHeaders, uriInfo);
        return Response.created(location).build();
    }

    private CatalogDirectory createCatalogDirectory(final eu.openanalytics.rsb.config.Configuration.Catalog catalogType,
            final HttpHeaders httpHeaders, final UriInfo uriInfo) throws IOException, URISyntaxException {

        final String catalogTypeAsString = catalogType.toString();
        final File configuredDirectory = catalogType.getConfiguredDirectory(getConfiguration());

        final CatalogDirectory catalogDirectory = Util.REST_OBJECT_FACTORY.createCatalogDirectory();
        catalogDirectory.setType(catalogTypeAsString);
        catalogDirectory.setPath(configuredDirectory.getCanonicalPath());

        for (final File file : catalogType.getConfiguredDirectory(getConfiguration()).listFiles(Constants.FILE_ONLY_FILTER)) {
            final URI dataUri = buildCatalogFileUri(catalogType, file, httpHeaders, uriInfo);
            final CatalogFileType catalogFile = Util.REST_OBJECT_FACTORY.createCatalogFileType();
            catalogFile.setName(file.getName());
            catalogFile.setDataUri(dataUri.toString());
            catalogDirectory.getFiles().add(catalogFile);
        }

        return catalogDirectory;
    }

    public URI buildCatalogFileUri(final eu.openanalytics.rsb.config.Configuration.Catalog catalogType, final File file,
            final HttpHeaders httpHeaders, final UriInfo uriInfo) throws URISyntaxException {

        return Util.getUriBuilder(uriInfo, httpHeaders).path(Constants.ADMIN_PATH).path(CATALOG_SUBPATH).path(catalogType.toString())
                .path(file.getName()).build();
    }
}
