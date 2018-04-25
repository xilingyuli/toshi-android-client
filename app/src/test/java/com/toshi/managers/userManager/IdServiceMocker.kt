/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 *  This program is free software: you can redistribute it and/or modify
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

import android.content.Context
import com.toshi.manager.network.IdInterface
import com.toshi.manager.network.IdService
import com.toshi.model.local.User
import com.toshi.model.network.ServerTime
import com.toshi.model.network.UserDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import rx.Single
import java.io.File

class IdServiceMocker {
    fun mock(toshiId: String = "0x0"): IdService {
        val idApi = Mockito.mock(IdInterface::class.java)
        val context = mockContext()
        val idService = IdService(idApi, context)
        Mockito.`when`(idApi.timestamp)
                .thenReturn(Single.just(ServerTime(1L)))
        Mockito.`when`(idApi.registerUser(any(UserDetails::class.java), any(Long::class.java)))
                .thenReturn(Single.just(User(toshiId)))
        Mockito.`when`(idApi.getUser(any(String::class.java)))
                .thenReturn(Single.just(User(toshiId)))
        return idService
    }

    private fun mockContext(): Context {
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.cacheDir)
                .thenReturn(File(""))
        return context
    }
}