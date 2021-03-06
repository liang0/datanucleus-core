/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.types.converters.ColumnLengthDefiningTypeConverter;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.time.LocalTime and a String form.
 */
public class LocalTimeStringConverter implements TypeConverter<LocalTime, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = 4942570075844340588L;

    public LocalTime toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return LocalTime.parse(str);
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, LocalTime.class.getName()), pe);
        }
    }

    public String toDatastoreType(LocalTime date)
    {
        return date != null ? date.toString() : null;
    }

    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        // Persist as "hh:mm:ss.SSS" when stored as string
        return 12;
    }
}
