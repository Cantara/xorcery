package com.exoreaction.reactiveservices.service.mapdbeventlog;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import jakarta.ws.rs.ext.Provider;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

public class MapDbEventLogService
{
//    @Provider
    public static class Feature
        extends AbstractFeature
    {
        @Override
        protected String serviceType() {
            return "mapdbeventlog";
        }

        @Override
        protected void configure() {

        }
    }

    public MapDbEventLogService()
    {
        DB db = DBMaker.memoryDB().make();

        BTreeMap<Long, byte[]> byPosition = db.treeMap("byposition", Serializer.LONG_DELTA, Serializer.BYTE_ARRAY)
                .counterEnable()
                .createOrOpen();

        BTreeMap<String, Long> byId = db.treeMap("byid", Serializer.STRING, Serializer.LONG)
                .counterEnable()
                .createOrOpen();



    }
}
