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

package com.toshi.manager.store

import io.realm.RealmModel
import io.realm.Sort

interface ToshiDBInterface {
    fun beginTransaction()
    fun commitTransaction()
    fun open()
    fun close()
    fun insertOrUpdate(item: RealmModel)
    fun <E : RealmModel> findFirst(clazz: Class<E>): E?
    fun <E : RealmModel> findFirstEqualTo(clazz: Class<E>, fieldName: String, value: String): E?
    fun <E : RealmModel> deleteFirstEqualTo(clazz: Class<E>, fieldName: String, value: String)
    fun <E : RealmModel> copyFromRealm(realmObject: E): E
    fun <E : RealmModel> copyFromRealm(realmObject: List<E>): List<E>
    fun <E : RealmModel> copyToRealmOrUpdate(realmObject: E): E
    fun <E : RealmModel> findFirstGreaterThan(clazz: Class<E>, greaterThanFieldName: String, value: Int): E?
    fun <E : RealmModel> findAllNotEmptyEqualTo(clazz: Class<E>,
                                        fieldName: String,
                                        value: Boolean,
                                        isNotEmptyField: String,
                                        sortAfterField: String,
                                        sortOrder: Sort): List<E>
}