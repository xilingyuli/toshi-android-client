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

package com.toshi.model.local;


import com.squareup.moshi.Json;
import com.toshi.model.network.user.UserType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class User extends RealmObject {

    private final static int CACHE_TIMEOUT = 1000 * 60 * 5;

    @PrimaryKey
    @Json(name = "toshi_id")
    private String owner_address;
    private String payment_address;
    private String username;
    private long cacheTimestamp;
    private Double reputation_score;
    private Double average_rating;
    private int review_count;
    @Json(name = "description")
    private String about;
    private String avatar;
    private String location;
    private String name;
    @Json(name = "public")
    private boolean is_public;
    private String type;

    // ctors
    public User() {
        this.cacheTimestamp = System.currentTimeMillis();
    }

    public User(final String toshiId) {
        this.owner_address = toshiId;
        this.cacheTimestamp = System.currentTimeMillis();
    }

    // Getters

    public String getUsername() {
        return String.format("@%s", username);
    }

    public String getUsernameForEditing() {
        return this.username;
    }

    // Defaults to the username if no name is set.
    public String getDisplayName() {
        if (this.name != null && this.name.length() > 0) {
            return this.name;
        }
        return username;
    }

    public String getToshiId() {
        return owner_address;
    }

    public String getPaymentAddress() {
        return payment_address;
    }

    @Nullable
    public String getAbout() {
        return this.about;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public String getLocation() {
        return this.location;
    }

    // Setters

    public void setUsername(final String username) {
        this.username = username;
    }

    public boolean needsRefresh() {
        return System.currentTimeMillis() - cacheTimestamp > CACHE_TIMEOUT;
    }

    public Double getReputationScore() {
        if (this.reputation_score == null) return 0.0;
        return this.reputation_score;
    }

    public Double getAverageRating() {
        if (this.average_rating == null) return 0.0;
        return this.average_rating;
    }

    public int getReviewCount() {
        return review_count;
    }

    @NotNull
    public UserType getType() {
        return type != null ? UserType.Companion.get(type) : UserType.USER;
    }

    public boolean isBot() {
        final UserType userType = UserType.Companion.get(type);
        return userType == UserType.BOT;
    }

    public boolean isPublic() {
        return is_public;
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof User)) return false;
        final User otherUser = (User) other;
        return otherUser.getToshiId().equals(this.getToshiId());
    }

    @Override
    public int hashCode() {
        return getToshiId().hashCode();
    }
}
