//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.cali;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wire.bots.sdk.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
class Config extends Configuration {
    public String secretPath;
    public String redirect;
    public String ingress;
    public int portMin;
    public int portMax;
    public String module;
    public DB postgres;

    public String getRedirect() {
        return redirect;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public String getIngress() {
        return ingress;
    }

    public int getPortMin() {
        return portMin;
    }

    public int getPortMax() {
        return portMax;
    }

    public String getModule() {
        return module;
    }

    public DB getPostgres() {
        return postgres;
    }
}
