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

import com.toshi.view.BaseApplication
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm

class ToshiDB : ToshiDBInterface {

    private lateinit var realm: Realm

    override fun open() {
        realm = BaseApplication.get().realm
    }

    override fun close() = realm.close()

    override fun beginTransaction() = realm.beginTransaction()

    override fun commitTransaction() = realm.commitTransaction()

    override fun <E : RealmModel> findFirst(clazz: Class<E>): E? {
        return realm.where(clazz).findFirst()
    }

    override fun <E : RealmModel> findFirstEqualTo(clazz: Class<E>, fieldName: String, value: String): E? {
        return realm.where(clazz)
                .equalTo(fieldName, value)
                .findFirst()
    }

    override fun <E : RealmModel> deleteFirstEqualTo(clazz: Class<E>, fieldName: String, value: String) {
        realm.where(clazz)
                .equalTo(fieldName, value)
                .findFirst()
                ?.deleteFromRealm()
    }

    override fun <E : RealmModel> copyFromRealm(realmObject: E): E {
        return realm.copyFromRealm(realmObject)
    }

    override fun <E : RealmModel> copyFromRealm(realmObject: List<E>): List<E> {
        return realm.copyFromRealm(realmObject)
    }

    override fun <E : RealmModel> copyToRealmOrUpdate(realmObject: E): E {
        return realm.copyToRealmOrUpdate(realmObject)
    }

    override fun insertOrUpdate(realmObject: RealmModel) {
        realm.insertOrUpdate(realmObject)
    }

    override fun <E : RealmModel> findAllNotEmptyEqualTo(clazz: Class<E>,
                                                         fieldName: String,
                                                         value: Boolean,
                                                         isNotEmptyField: String,
                                                         sortAfterField: String,
                                                         sortOrder: Sort): RealmResults<E> {
        return realm.where(clazz)
                .equalTo(fieldName, value)
                .isNotEmpty(isNotEmptyField)
                .sort(sortAfterField, sortOrder)
                .findAll()
    }

    override fun <E : RealmModel> findFirstGreaterThan(clazz: Class<E>, greaterThanFieldName: String, value: Int): E? {
        return realm.where(clazz)
                .greaterThan(greaterThanFieldName, value)
                .findFirst()
    }
}