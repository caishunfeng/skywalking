/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.storage.es.dao;

import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.skywalking.apm.collector.storage.dao.IApplicationReferenceMetricPersistenceDAO;
import org.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.skywalking.apm.collector.storage.table.noderef.ApplicationReferenceMetric;
import org.skywalking.apm.collector.storage.table.noderef.NodeReferenceTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class ApplicationReferenceEsMetricPersistenceDAO extends EsDAO implements IApplicationReferenceMetricPersistenceDAO<IndexRequestBuilder, UpdateRequestBuilder, ApplicationReferenceMetric> {

    private final Logger logger = LoggerFactory.getLogger(ApplicationReferenceEsMetricPersistenceDAO.class);

    public ApplicationReferenceEsMetricPersistenceDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override public ApplicationReferenceMetric get(String id) {
        GetResponse getResponse = getClient().prepareGet(NodeReferenceTable.TABLE, id).get();
        if (getResponse.isExists()) {
            ApplicationReferenceMetric applicationReferenceMetric = new ApplicationReferenceMetric(id);
            Map<String, Object> source = getResponse.getSource();
            applicationReferenceMetric.setFrontApplicationId(((Number)source.get(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID)).intValue());
            applicationReferenceMetric.setBehindApplicationId(((Number)source.get(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID)).intValue());
            applicationReferenceMetric.setS1Lte(((Number)source.get(NodeReferenceTable.COLUMN_S1_LTE)).intValue());
            applicationReferenceMetric.setS3Lte(((Number)source.get(NodeReferenceTable.COLUMN_S3_LTE)).intValue());
            applicationReferenceMetric.setS5Lte(((Number)source.get(NodeReferenceTable.COLUMN_S5_LTE)).intValue());
            applicationReferenceMetric.setS5Gt(((Number)source.get(NodeReferenceTable.COLUMN_S5_GT)).intValue());
            applicationReferenceMetric.setSummary(((Number)source.get(NodeReferenceTable.COLUMN_SUMMARY)).intValue());
            applicationReferenceMetric.setError(((Number)source.get(NodeReferenceTable.COLUMN_ERROR)).intValue());
            applicationReferenceMetric.setTimeBucket(((Number)source.get(NodeReferenceTable.COLUMN_TIME_BUCKET)).longValue());
            return applicationReferenceMetric;
        } else {
            return null;
        }
    }

    @Override public IndexRequestBuilder prepareBatchInsert(ApplicationReferenceMetric data) {
        Map<String, Object> source = new HashMap<>();
        source.put(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID, data.getFrontApplicationId());
        source.put(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID, data.getBehindApplicationId());
        source.put(NodeReferenceTable.COLUMN_S1_LTE, data.getS1Lte());
        source.put(NodeReferenceTable.COLUMN_S3_LTE, data.getS3Lte());
        source.put(NodeReferenceTable.COLUMN_S5_LTE, data.getS5Lte());
        source.put(NodeReferenceTable.COLUMN_S5_GT, data.getS5Gt());
        source.put(NodeReferenceTable.COLUMN_SUMMARY, data.getSummary());
        source.put(NodeReferenceTable.COLUMN_ERROR, data.getError());
        source.put(NodeReferenceTable.COLUMN_TIME_BUCKET, data.getTimeBucket());

        return getClient().prepareIndex(NodeReferenceTable.TABLE, data.getId()).setSource(source);
    }

    @Override public UpdateRequestBuilder prepareBatchUpdate(ApplicationReferenceMetric data) {
        Map<String, Object> source = new HashMap<>();
        source.put(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID, data.getFrontApplicationId());
        source.put(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID, data.getBehindApplicationId());
        source.put(NodeReferenceTable.COLUMN_S1_LTE, data.getS1Lte());
        source.put(NodeReferenceTable.COLUMN_S3_LTE, data.getS3Lte());
        source.put(NodeReferenceTable.COLUMN_S5_LTE, data.getS5Lte());
        source.put(NodeReferenceTable.COLUMN_S5_GT, data.getS5Gt());
        source.put(NodeReferenceTable.COLUMN_SUMMARY, data.getSummary());
        source.put(NodeReferenceTable.COLUMN_ERROR, data.getError());
        source.put(NodeReferenceTable.COLUMN_TIME_BUCKET, data.getTimeBucket());

        return getClient().prepareUpdate(NodeReferenceTable.TABLE, data.getId()).setDoc(source);
    }

    @Override public void deleteHistory(Long startTimestamp, Long endTimestamp) {
        long startTimeBucket = TimeBucketUtils.INSTANCE.getMinuteTimeBucket(startTimestamp);
        long endTimeBucket = TimeBucketUtils.INSTANCE.getMinuteTimeBucket(endTimestamp);
        BulkByScrollResponse response = getClient().prepareDelete()
            .filter(QueryBuilders.rangeQuery(NodeReferenceTable.COLUMN_TIME_BUCKET).gte(startTimeBucket).lte(endTimeBucket))
            .source(NodeReferenceTable.TABLE)
            .get();

        long deleted = response.getDeleted();
        logger.info("Delete {} rows history from {} index.", deleted, NodeReferenceTable.TABLE);
    }
}