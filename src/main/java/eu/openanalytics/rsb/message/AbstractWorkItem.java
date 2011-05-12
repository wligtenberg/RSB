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
package eu.openanalytics.rsb.message;

import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import eu.openanalytics.rsb.Util;

/**
 * Parent of all the work item messages.
 * 
 * @author "OpenAnalytics <rsb.development@openanalytics.eu>"
 */
public abstract class AbstractWorkItem implements Serializable {
    public enum Source {
        REST("job.error", "job.abort"), SOAP("job.error", "job.abort"), EMAIL("email.job.error", "email.job.abort"), DIRECTORY(
                "directory.job.error", "directory.job.abort");

        private final String errorMessageId;
        private final String abortMessageId;

        private Source(final String errorMessageId, final String abortMessageId) {
            this.errorMessageId = errorMessageId;
            this.abortMessageId = abortMessageId;
        }
    };

    private static final long serialVersionUID = 1L;

    private final Source source;
    private final String applicationName;
    private final UUID jobId;
    private final GregorianCalendar submissionTime;
    private final Map<String, Serializable> meta;

    public AbstractWorkItem(final Source source, final String applicationName, final UUID jobId, final GregorianCalendar submissionTime,
            final Map<String, Serializable> meta) {
        Validate.notNull(source, "source can't be null");
        Validate.notEmpty(applicationName, "applicationName can't be empty");
        Validate.notNull(jobId, "jobId can't be null");
        Validate.notNull(submissionTime, "submissionTime can't be null");
        Validate.notNull(meta, "meta can't be null");

        if (!Util.isValidApplicationName(applicationName)) {
            throw new IllegalArgumentException("Invalid application name: " + applicationName);
        }

        this.source = source;
        this.applicationName = applicationName;
        this.jobId = jobId;
        this.submissionTime = submissionTime;
        this.meta = meta;
    }

    public void destroy() {
        releaseResources();
    }

    protected abstract void releaseResources();

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public Source getSource() {
        return source;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public UUID getJobId() {
        return jobId;
    }

    public GregorianCalendar getSubmissionTime() {
        return submissionTime;
    }

    public Map<String, Serializable> getMeta() {
        return meta;
    }

    public String getErrorMessageId() {
        return getSource().errorMessageId;
    }

    public String getAbortMessageId() {
        return getSource().abortMessageId;
    }

}
