/*
 *   R Service Bus
 *   
 *   Copyright (c) Copyright of OpenAnalytics BVBA, 2010-2013
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

package eu.openanalytics.rsb.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import eu.openanalytics.rsb.config.Configuration;
import eu.openanalytics.rsb.config.Configuration.ApplicationSecurityAuthorization;

/**
 * Defines a {@link PermissionEvaluator} that considers the applications a user is
 * granted to use.
 * 
 * @author "OpenAnalytics &lt;rsb.development@openanalytics.eu&gt;"
 */
public class ApplicationPermissionEvaluator implements PermissionEvaluator
{
    public static final String NO_AUTHENTICATED_USERNAME = null;

    private static final Set<String> SUPPORTED_PERMISSIONS = new HashSet<String>(
        Collections.singleton("APPLICATION_USER"));

    @Resource
    private Configuration configuration;

    @Override
    public boolean hasPermission(final Authentication authentication,
                                 final Object targetDomainObject,
                                 final Object permission)
    {
        final Map<String, ApplicationSecurityAuthorization> applicationSecurityConfiguration = configuration.getApplicationSecurityConfiguration();

        if ((applicationSecurityConfiguration == null) || (targetDomainObject == null)
            || (!SUPPORTED_PERMISSIONS.contains(permission)))
        {
            return false;
        }

        final String applicationName = targetDomainObject.toString();

        final ApplicationSecurityAuthorization applicationSecurityAuthorization = applicationSecurityConfiguration.get(applicationName);
        if (applicationSecurityAuthorization == null)
        {
            return false;
        }

        final String userName = getUserName(authentication);

        if ((StringUtils.isNotBlank(userName))
            && (!CollectionUtils.isEmpty(applicationSecurityAuthorization.getAuthorizedPrincipals()))
            && (applicationSecurityAuthorization.getAuthorizedPrincipals().contains(userName)))
        {
            return true;
        }

        final Set<String> roles = new HashSet<String>();
        for (final GrantedAuthority authority : authentication.getAuthorities())
        {
            roles.add(authority.getAuthority());
        }

        return CollectionUtils.containsAny(applicationSecurityAuthorization.getAuthorizedRoles(), roles);
    }

    private String getUserName(final Authentication authentication)
    {
        if (authentication.getPrincipal() instanceof UserDetails)
        {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean hasPermission(final Authentication authentication,
                                 final Serializable targetId,
                                 final String targetType,
                                 final Object permission)
    {
        throw new UnsupportedOperationException(
            "Application permission verification can only be done on objects");
    }
}
