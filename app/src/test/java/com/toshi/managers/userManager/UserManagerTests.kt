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

package com.toshi.managers.userManager

import com.toshi.manager.UserManager
import com.toshi.managers.recipientManager.RecipientManagerMocker
import com.toshi.storage.TestUserPrefs
import junit.framework.Assert.assertNull
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test

class UserManagerTests {

    private lateinit var userPrefs: TestUserPrefs
    private lateinit var userManager: UserManager

    @Before
    fun before() {
        userPrefs = TestUserPrefs()
        userManager = UserManagerMocker().mock(
                userPrefs = userPrefs,
                recipientManager = RecipientManagerMocker().mock()
        )
    }

    @Test
    fun `check if user prefs returns null when prefs is empty`() {
        userPrefs.clear()
        assertThatPrefsAreCleared()
    }

    @Test
    fun `check if user prefs are empty after clear`() {
        userPrefs.setUserId("1")
        userPrefs.clear()
        assertThatPrefsAreCleared()
    }

    @Test
    fun `check if everthing is cleared`() {
        userPrefs.setUserId("1")
        userManager.clear()
        assertThatPrefsAreCleared()
    }

    private fun assertThatPrefsAreCleared() {
        val userId = userPrefs.getUserId()
        val oldUserId = userPrefs.getOldUserId()
        assertNull(userId)
        assertNull(oldUserId)
    }

    @Test
    fun `check if user prefs returns user id`() {
        userPrefs.clear()
        userPrefs.setUserId("1")
        val userId = userPrefs.getUserId()
        MatcherAssert.assertThat(userId, Matchers.`is`("1"))
    }

    @Test
    fun `test getting user before init`() {
        val currentUser = userManager.getCurrentUser().toBlocking().value()
        assertNull(currentUser)
    }

    // TODO Mock RecipientManager in UserManager. Not possible atm because ToshiManager is null
}