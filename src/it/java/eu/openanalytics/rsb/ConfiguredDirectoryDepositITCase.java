/*
 *   R Service Bus
 *
 *   Copyright (c) Copyright of Open Analytics NV, 2010-2015
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

package eu.openanalytics.rsb;

import java.io.IOException;

import org.junit.Test;

/**
 * @author "OpenAnalytics &lt;rsb.development@openanalytics.eu&gt;"
 */
public class ConfiguredDirectoryDepositITCase extends DirectoryDepositITCase
{
    @Override
    protected String getApplicationName()
    {
        return "lab2";
    }

    @Override
    @Test
    public void dataOnlyZipDeposit() throws IOException, InterruptedException
    {
        doTestValidZipDeposit("r-job-data-only.zip");
    }
}
