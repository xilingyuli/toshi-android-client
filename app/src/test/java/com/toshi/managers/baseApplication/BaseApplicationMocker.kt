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

package com.toshi.managers.baseApplication

import android.content.res.Resources
import com.toshi.view.BaseApplication
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import rx.subjects.BehaviorSubject
import java.io.InputStream

class BaseApplicationMocker {
    fun mock(): BaseApplication {
        val baseApplication = Mockito.mock(BaseApplication::class.java)
        val subject = BehaviorSubject.create<Boolean>()
        subject.onNext(true)
        Mockito.`when`(baseApplication.isConnectedSubject)
                .thenReturn(subject)

        val resources = Mockito.mock(Resources::class.java)
        Mockito.`when`(resources.openRawResource(any(Int::class.java)))
                .thenReturn(object : InputStream() {
                    override fun read(): Int = 1
                })

        Mockito.`when`(baseApplication.resources)
                .thenReturn(resources)

        return baseApplication
    }
}