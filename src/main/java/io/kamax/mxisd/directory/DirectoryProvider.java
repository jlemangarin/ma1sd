/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.directory;

import io.kamax.mxisd.http.io.UserDirectorySearchResult;

public interface DirectoryProvider {

    UserDirectorySearchResult searchByDisplayName(String query);

    UserDirectorySearchResult searchBy3pid(String query);
    
    UserDirectorySearchResult searchByDisplayNameGofast(String query, String accessToken);

    UserDirectorySearchResult searchBy3pidGofast(String query, String accessToken);

}
