/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.storage

import com.toshi.util.sharedPrefs.UserPrefsInterface

class TestUserPrefs : UserPrefsInterface {

    companion object {
        private const val OLD_USER_ID = "uid"
        private const val USER_ID = "uid_v2"
    }

    private val map by lazy { HashMap<String, Any?>() }

    override fun getOldUserId(): String? = map[OLD_USER_ID] as String?

    override fun getUserId(): String? = map[USER_ID] as String?

    override fun setUserId(userId: String) {
        map[USER_ID] = userId
    }

    override fun clear() = map.clear()
}