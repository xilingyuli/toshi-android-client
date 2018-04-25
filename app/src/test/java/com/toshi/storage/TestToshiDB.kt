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

package com.toshi.storage

import com.toshi.manager.store.ToshiDBInterface
import io.realm.RealmModel
import io.realm.Sort

class TestToshiDB : ToshiDBInterface {

    override fun open() {}

    private val db by lazy { mutableListOf<Any>() }

    override fun beginTransaction() {}

    override fun commitTransaction() {}

    override fun close() {}

    override fun <E : RealmModel> findFirst(clazz: Class<E>): E? {
        return db.firstOrNull { clazz.isInstance(it) } as? E
    }

    override fun <E : RealmModel> findFirstEqualTo(clazz: Class<E>, fieldName: String, value: String): E? {
        return db.firstOrNull { clazz.isInstance(it) } as? E
    }

    override fun <E : RealmModel> deleteFirstEqualTo(clazz: Class<E>, fieldName: String, value: String) {
        val itemToDelete = db.first { clazz.isInstance(it) } as? E ?: return
        db.remove(itemToDelete)
    }

    override fun <E : RealmModel> copyFromRealm(item: E): E {
        insertOrUpdate(item)
        return item
    }

    override fun <E : RealmModel> copyFromRealm(items: List<E>): List<E> {
        items.forEach { insertOrUpdate(it) }
        return items
    }

    override fun <E : RealmModel> copyToRealmOrUpdate(item: E): E {
        insertOrUpdate(item)
        return item
    }

    override fun insertOrUpdate(item: RealmModel) {
        if (db.contains(item)) {
            val indexOf = db.indexOf(item)
            db[indexOf] = item
        } else {
            db.add(item)
        }
    }

    override fun <E : RealmModel> findFirstGreaterThan(clazz: Class<E>, greaterThanFieldName: String, value: Int): E? {
        return db.firstOrNull { clazz.isInstance(it) } as? E
    }

    override fun <E : RealmModel> findAllNotEmptyEqualTo(clazz: Class<E>, fieldName: String, value: Boolean, isNotEmptyField: String, sortAfterField: String, sortOrder: Sort): List<E> {
        return try {
            db.filter { clazz.isInstance(it) } as List<E>
        } catch (e: NoSuchElementException) {
            return emptyList()
        }
    }


}