/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.store.types.converters;

/**
 * Class to handle the conversion between java.lang.Boolean and a Character ("Y", "N") form.
 */
public class BooleanYNConverter implements TypeConverter<Boolean, Character>
{
    private static final long serialVersionUID = 778758633106246559L;

    public Boolean toMemberType(Character chr)
    {
        if (chr == null)
        {
            return null;
        }

        return chr.equals('Y') ? true : false;
    }

    public Character toDatastoreType(Boolean bool)
    {
        return bool != null ? (bool ? 'Y' : 'N') : null;
    }
}