/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License,
 * attached with Common Clause Condition 1.0, found in the LICENSES directory.
 */

package com.vesoft.nebula.client.storage;

import com.vesoft.nebula.HostAddr;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.meta.MetaManager;
import com.vesoft.nebula.client.storage.scan.PartScanInfo;
import com.vesoft.nebula.client.storage.scan.ScanEdgeResultIterator;
import com.vesoft.nebula.client.storage.scan.ScanVertexResultIterator;
import com.vesoft.nebula.meta.ColumnDef;
import com.vesoft.nebula.meta.Schema;
import com.vesoft.nebula.storage.EdgeProp;
import com.vesoft.nebula.storage.ScanEdgeRequest;
import com.vesoft.nebula.storage.ScanVertexRequest;
import com.vesoft.nebula.storage.VertexProp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);

    private final GraphStorageConnection connection;
    private StorageConnPool pool;
    private MetaManager metaManager;
    private final List<HostAddress> addresses;
    private int timeout = 10000; // ms

    /**
     * Get a Nebula Storage client that executes the scan query to get NebulaGraph's data with
     * one server host.
     *
     * @param ip   the ip of metad server
     * @param port the port of metad server
     */
    public StorageClient(String ip, int port) {
        this(Arrays.asList(new HostAddress(ip, port)));
    }

    /**
     * Get a Nebula Storage client that executes the scan query to get NebulaGraph's data with multi
     * servers' host.
     *
     * @param addresses the {@link HostAddress} list of metad servers
     */
    public StorageClient(List<HostAddress> addresses) {
        this.connection = new GraphStorageConnection();
        this.addresses = addresses;
    }

    /**
     * Get a Nebula Storage client that executes the scan query to get NebulaGraph's data with
     * multi servers' hosts and timeout.
     *
     * @param addresses the {@link HostAddress} list of metad servers
     * @param timeout   the timeout of scan vertex or edge
     */
    public StorageClient(List<HostAddress> addresses, int timeout) {
        this.connection = new GraphStorageConnection();
        this.addresses = addresses;
        this.timeout = timeout;
    }

    /**
     * Connect to Nebula Storage server.
     *
     * @return true if connect successfully.
     */
    public boolean connect() throws Exception {
        connection.open(addresses.get(0), timeout);
        StoragePoolConfig config = new StoragePoolConfig();
        pool = new StorageConnPool(config);
        metaManager = MetaManager.getMetaManager(addresses);
        return true;
    }


    /**
     * scan vertex of all parts with specific return cols, if returnCols is an empty list, then
     * return all the columns of specific tagName.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName  Nebula space name
     * @param tagName    Nebula tag name
     * @param returnCols Nebula tag properties to return
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName, String tagName,
                                               List<String> returnCols) {
        return scanVertex(spaceName, tagName, returnCols, DEFAULT_LIMIT);
    }

    /**
     * scan vertex of specific part with specific return cols, if returnCols is an empty list,
     * then return all the columns of specific tagName.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName  Nebula space name
     * @param part       Nebula data partition
     * @param tagName    Nebula tag name
     * @param returnCols Nebula tag properties to return
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName, int part, String tagName,
                                               List<String> returnCols) {
        return scanVertex(spaceName, part, tagName, returnCols, DEFAULT_LIMIT);
    }

    /**
     * scan vertex of all parts with no return cols.
     *
     * <p>the result only contains vertex id.
     *
     * @param spaceName Nebula space name
     * @param tagName   Nebula tag name
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName, String tagName) {
        return scanVertex(spaceName, tagName, DEFAULT_LIMIT);
    }

    /**
     * scan vertex of specific part with no return cols.
     *
     * <p>the result only contains vertex id.
     *
     * @param spaceName Nebula space name
     * @param part      Nebula data partition
     * @param tagName   Nebula tag name
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName, int part, String tagName) {
        return scanVertex(spaceName, part, tagName, DEFAULT_LIMIT);
    }

    /**
     * scan vertex of all parts with specific return cols and limit.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName  Nebula space name
     * @param tagName    Nebula tag name
     * @param returnCols Nebula tag properties to return
     * @param limit      the data amount of scan in every iterator
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               String tagName,
                                               List<String> returnCols,
                                               int limit) {
        return scanVertex(spaceName, tagName, returnCols, limit, DEFAULT_START_TIME,
                DEFAULT_END_TIME);
    }

    /**
     * scan vertex of specific part with specific return cols and limit.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName  Nebula space name
     * @param part       Nebula data partition
     * @param tagName    Nebula tag name
     * @param returnCols Nebula tag properties to return
     * @param limit      the data amount of scan in every iterator
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               int part,
                                               String tagName,
                                               List<String> returnCols,
                                               int limit) {
        return scanVertex(spaceName, part, tagName, returnCols, limit, DEFAULT_START_TIME,
                DEFAULT_END_TIME);
    }

    /**
     * scan vertex of all parts with no return cols and limit.
     *
     * <p>the result only contains vertex id.
     *
     * @param spaceName Nebula space name
     * @param tagName   Nebula tag name
     * @param limit     the data amount of scan in every iterator
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               String tagName,
                                               int limit) {
        return scanVertex(spaceName, tagName, limit, DEFAULT_START_TIME, DEFAULT_END_TIME);
    }

    /**
     * scan vertex of specific part with no return cols and limit.
     *
     * <p>the result only contains vertex id.
     *
     * @param spaceName Nebula space name
     * @param part      Nebula data partition
     * @param tagName   Nebula tag name
     * @param limit     the data amount of scan in every iterator
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               int part,
                                               String tagName,
                                               int limit) {
        return scanVertex(spaceName, part, tagName,
                limit, DEFAULT_START_TIME, DEFAULT_END_TIME);
    }

    /**
     * scan vertex of all parts with specific returnCols, limit, startTime and endTime.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName  Nebula space name
     * @param tagName    Nebula tag name
     * @param returnCols nebula tag properties to return
     * @param limit      the data amount of scan operation for each partition in every iterator
     * @param startTime  the time range's start time of the data to be scanned, if data was insert
     *                   before start time, then it will not be return.
     * @param endTime    the time range's end time of the data to be scanned, if data was insert
     *                   after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               String tagName,
                                               List<String> returnCols,
                                               int limit,
                                               long startTime,
                                               long endTime) {
        return scanVertex(spaceName, tagName, returnCols, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }

    /**
     * scan vertex of specific part with specific returnCols, limit, startTime and endTime.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName  Nebula space name
     * @param part       Nebula data partition
     * @param tagName    Nebula tag name
     * @param returnCols Nebula tag properties to return
     * @param limit      the data amount of scan operation for each partition in every iterator
     * @param startTime  the time range's start time of the data to be scanned, if data was insert
     *                   before start time, then it will not be return.
     * @param endTime    the time range's end time of the data to be scanned, if data was insert
     *                   after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               int part,
                                               String tagName,
                                               List<String> returnCols,
                                               int limit,
                                               long startTime,
                                               long endTime) {
        return scanVertex(spaceName, part, tagName, returnCols, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }

    /**
     * scan vertex of all parts with no returnCols, limit, startTime and endTime.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName Nebula space name
     * @param tagName   Nebula tag name
     * @param limit     the data amount of scan operation for each partition in every iterator
     * @param startTime the time range's start time of the data to be scanned, if data was insert
     *                  before start time, then it will not be return.
     * @param endTime   the time range's end time of the data to be scanned, if data was insert
     *                  after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               String tagName,
                                               int limit,
                                               long startTime,
                                               long endTime) {
        return scanVertex(spaceName, tagName, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }

    /**
     * scan vertex of specific part with no returnCols, limit, startTime and endTime.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName Nebula space name
     * @param tagName   Nebula tag name
     * @param limit     the data amount of scan operation for each partition in every iterator
     * @param startTime the time range's start time of the data to be scanned, if data was insert
     *                  before start time, then it will not be return.
     * @param endTime   the time range's end time of the data to be scanned, if data was insert
     *                  after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               int part,
                                               String tagName,
                                               int limit,
                                               long startTime,
                                               long endTime) {
        return scanVertex(spaceName,
                part,
                tagName,
                new ArrayList<>(),
                limit,
                startTime,
                endTime,
                DEFAULT_ALLOW_PART_SUCCESS,
                DEFAULT_ALLOW_READ_FOLLOWER);
    }


    /**
     * scan vertex of all parts with specific return cols, limit, startTime, endTime， whether
     * allow partial success, whether allow read from storage follower.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName             Nebula graph space
     * @param tagName               Nebula tag name
     * @param returnCols            Nebula tag properties to return
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert
     *                              before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert
     *                              after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               String tagName,
                                               List<String> returnCols,
                                               int limit,
                                               long startTime,
                                               long endTime,
                                               boolean allowPartSuccess,
                                               boolean allowReadFromFollower) {
        List<Integer> parts = metaManager.getSpaceParts(spaceName);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("No valid part in space " + spaceName);
        }
        return scanVertex(
                spaceName,
                parts,
                tagName,
                returnCols,
                false,
                limit,
                startTime,
                endTime,
                allowPartSuccess,
                allowReadFromFollower);
    }


    /**
     * scan vertex of specific part with specific return cols, limit, startTime, endTime， whether
     * allow partial success, whether allow read from storage follower.
     *
     * <p>the result contains vertex id and return cols.
     *
     * @param spaceName             Nebula graph space
     * @param part                  Nebula data partition
     * @param tagName               Nebula tag name
     * @param returnCols            Nebula tag properties to return
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert
     *                              before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert
     *                              after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               int part,
                                               String tagName,
                                               List<String> returnCols,
                                               int limit,
                                               long startTime,
                                               long endTime,
                                               boolean allowPartSuccess,
                                               boolean allowReadFromFollower) {
        return scanVertex(
                spaceName,
                Arrays.asList(part),
                tagName,
                returnCols,
                false,
                limit,
                startTime,
                endTime,
                allowPartSuccess,
                allowReadFromFollower);
    }


    /**
     * scan vertex of all parts with no return cols, limit, startTime, endTime， whether
     * allow partial success, whether allow read from storage follower.
     *
     * <p>the result only contains vertex id.
     *
     * @param spaceName             Nebula graph space
     * @param tagName               Nebula tag name
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert
     *                              before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert
     *                              after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               String tagName,
                                               int limit,
                                               long startTime,
                                               long endTime,
                                               boolean allowPartSuccess,
                                               boolean allowReadFromFollower) {
        List<Integer> parts = metaManager.getSpaceParts(spaceName);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("No valid part in space " + spaceName);
        }
        return scanVertex(
                spaceName,
                parts,
                tagName,
                new ArrayList<>(),
                true,
                limit,
                startTime,
                endTime,
                allowPartSuccess,
                allowReadFromFollower);
    }

    /**
     * scan vertex of specific part with no return cols, limit, startTime, endTime， whether
     * allow partial success, whether allow read from storage follower.
     *
     * <p>the result only contains vertex id.
     *
     * @param spaceName             Nebula graph space
     * @param part                  Nebula data partition
     * @param tagName               Nebula tag name
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert
     *                              before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanVertexResultIterator scanVertex(String spaceName,
                                               int part,
                                               String tagName,
                                               int limit,
                                               long startTime,
                                               long endTime,
                                               boolean allowPartSuccess,
                                               boolean allowReadFromFollower) {
        return scanVertex(
                spaceName,
                Arrays.asList(part),
                tagName,
                new ArrayList<>(),
                true,
                limit,
                startTime,
                endTime,
                allowPartSuccess,
                allowReadFromFollower);
    }

    private ScanVertexResultIterator scanVertex(String spaceName,
                                                List<Integer> parts,
                                                String tagName,
                                                List<String> returnCols,
                                                boolean noColumns,
                                                int limit,
                                                long startTime,
                                                long endTime,
                                                boolean allowPartSuccess,
                                                boolean allowReadFromFollower) {
        if (spaceName == null || spaceName.trim().isEmpty()) {
            throw new IllegalArgumentException("space name is empty.");
        }
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("tag name is empty");
        }
        if (noColumns && returnCols == null) {
            throw new IllegalArgumentException("returnCols is null");
        }

        Set<PartScanInfo> partScanInfoSet = new HashSet<>();
        for (int part : parts) {
            HostAddr leader = metaManager.getLeader(spaceName, part);
            partScanInfoSet.add(new PartScanInfo(part, new HostAddress(leader.getHost(),
                    leader.getPort())));
        }
        List<HostAddress> addrs = new ArrayList<>();
        for (HostAddr addr : metaManager.listHosts()) {
            addrs.add(new HostAddress(addr.getHost(), addr.getPort()));
        }

        long tag = metaManager.getTag(spaceName, tagName).getTag_id();
        List<byte[]> props = new ArrayList<>();
        props.add("_vid".getBytes());
        if (!noColumns) {
            if (returnCols.size() == 0) {
                Schema schema = metaManager.getTag(spaceName, tagName).getSchema();
                for (ColumnDef columnDef : schema.getColumns()) {
                    props.add(columnDef.getName());
                }
            } else {
                for (String prop : returnCols) {
                    props.add(prop.getBytes());
                }
            }
        }
        VertexProp vertexCols = new VertexProp((int) tag, props);

        ScanVertexRequest request = new ScanVertexRequest();
        request
                .setSpace_id(getSpaceId(spaceName))
                .setReturn_columns(vertexCols)
                .setLimit(limit)
                .setStart_time(startTime)
                .setEnd_time(endTime)
                .setEnable_read_from_follower(allowReadFromFollower);

        return doScanVertex(spaceName, tagName, partScanInfoSet, request, addrs, allowPartSuccess);
    }


    /**
     * do scan vertex
     *
     * @param spaceName        Nebula space name
     * @param tagName          Nebula tag name
     * @param partScanInfoSet  leaders and scan cursors of all data partitions
     * @param request          {@link ScanVertexRequest}
     * @param addrs            storage address list
     * @param allowPartSuccess whether allow part success
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    private ScanVertexResultIterator doScanVertex(String spaceName,
                                                  String tagName,
                                                  Set<PartScanInfo> partScanInfoSet,
                                                  ScanVertexRequest request,
                                                  List<HostAddress> addrs,
                                                  boolean allowPartSuccess) {
        if (addrs == null || addrs.isEmpty()) {
            throw new IllegalArgumentException("storage hosts is empty.");
        }

        return new ScanVertexResultIterator.ScanVertexResultBuilder()
                .withMetaClient(metaManager)
                .withPool(pool)
                .withPartScanInfo(partScanInfoSet)
                .withRequest(request)
                .withAddresses(addrs)
                .withSpaceName(spaceName)
                .withTagName(tagName)
                .withPartSuccess(allowPartSuccess)
                .build();
    }


    /**
     * scan edge of all parts with return cols.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName  Nebula space name
     * @param edgeName   Nebula edge type name
     * @param returnCols Nebula edge properties to return
     * @return an iterator to get data by call {@link ScanVertexResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, String edgeName,
                                           List<String> returnCols) {

        return scanEdge(spaceName, edgeName, returnCols, DEFAULT_LIMIT);
    }

    /**
     * scan edge of specific part with return cols.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName  Nebula space name
     * @param part       Nebula data partition
     * @param edgeName   Nebula edge type name
     * @param returnCols Nebula edge properties to return
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, int part, String edgeName,
                                           List<String> returnCols) {

        return scanEdge(spaceName, part, edgeName, returnCols, DEFAULT_LIMIT);
    }

    /**
     * scan edge of all parts with no return cols.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName Nebula space name
     * @param edgeName  Nebula edge type name
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, String edgeName) {
        return scanEdge(spaceName, edgeName, DEFAULT_LIMIT);
    }

    /**
     * scan edge of specific part with no return cols.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName Nebula space name
     * @param part      Nebula data partition
     * @param edgeName  Nebula edge type name
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, int part, String edgeName) {
        return scanEdge(spaceName, part, edgeName, DEFAULT_LIMIT);
    }

    /**
     * scan edge of all parts with return cols and limit config.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName  Nebula space name
     * @param edgeName   Nebula edge type name
     * @param returnCols Nebula edge properties to return
     * @param limit      the data amount of scan operation for each partition in every
     *                   iterator
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, String edgeName,
                                           List<String> returnCols, int limit) {
        return scanEdge(spaceName, edgeName, returnCols, limit, DEFAULT_START_TIME,
                DEFAULT_END_TIME);
    }

    /**
     * scan edge of specific part with return cols.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName  Nebula space name
     * @param edgeName   Nebula edge type name
     * @param returnCols Nebula edge properties to return
     * @param limit      the data amount of scan operation for each partition in every
     *                   iterator
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, int part, String edgeName,
                                           List<String> returnCols, int limit) {
        return scanEdge(spaceName, part, edgeName, returnCols, limit, DEFAULT_START_TIME,
                DEFAULT_END_TIME);
    }

    /**
     * scan edge of all parts with no return cols and limit config.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName Nebula space name
     * @param edgeName  Nebula edge type name
     * @param limit     the data amount of scan operation for each partition in every
     *                  iterator
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, String edgeName, int limit) {
        return scanEdge(spaceName, edgeName, limit, DEFAULT_START_TIME, DEFAULT_END_TIME);
    }

    /**
     * scan edge of specific part with no return cols and limit config.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName Nebula space name
     * @param part      Nebula data partition
     * @param edgeName  Nebula edge type name
     * @param limit     the data amount of scan operation for each partition in every
     *                  iterator
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName, int part, String edgeName, int limit) {
        return scanEdge(spaceName, part, edgeName, limit, DEFAULT_START_TIME, DEFAULT_END_TIME);
    }

    /**
     * scan edge of all parts with return cols and limit, start time, end time config.
     *
     * <p>the result only contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName  Nebula space name
     * @param edgeName   Nebula edge type name
     * @param returnCols Nebula edge properties to return
     * @param limit      the data amount of scan operation for each partition in every
     *                   iterator
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           String edgeName,
                                           List<String> returnCols,
                                           int limit,
                                           long startTime,
                                           long endTime) {
        return scanEdge(spaceName, edgeName, returnCols, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }

    /**
     * scan edge of specific part with return cols and limit, start time, end time config.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName  Nebula space name
     * @param part       Nebula data partition
     * @param edgeName   Nebula edge type name
     * @param returnCols Nebula edge properties to return
     * @param limit      the data amount of scan operation for each partition in every
     *                   iterator
     * @param startTime  the time range's start time of the data to be scanned, if
     *                   data was insert before start time, then it will not be return.
     * @param endTime    the time range's end time of the data to be scanned, if data
     *                   was insert after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           int part,
                                           String edgeName,
                                           List<String> returnCols,
                                           int limit,
                                           long startTime,
                                           long endTime) {
        return scanEdge(spaceName, part, edgeName, returnCols, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }

    /**
     * scan edge of all parts with no return cols and limit, start time, end time config.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName Nebula space name
     * @param edgeName  Nebula edge type name
     * @param limit     the data amount of scan operation for each partition in every
     *                  iterator
     * @param startTime the time range's start time of the data to be scanned, if
     *                  data was insert before start time, then it will not be return.
     * @param endTime   the time range's end time of the data to be scanned, if data
     *                  was insert after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           String edgeName,
                                           int limit,
                                           long startTime,
                                           long endTime) {
        return scanEdge(spaceName, edgeName, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }

    /**
     * scan edge of specific part with no return cols and limit, start time, end time config.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName Nebula space name
     * @param part      Nebula data partition
     * @param edgeName  Nebula edge type name
     * @param limit     the data amount of scan operation for each partition in every
     *                  iterator
     * @param startTime the time range's start time of the data to be scanned, if
     *                  data was insert before start time, then it will not be return.
     * @param endTime   the time range's end time of the data to be scanned, if data
     *                  was insert after end time, then it will not be return.
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           int part,
                                           String edgeName,
                                           int limit,
                                           long startTime,
                                           long endTime) {
        return scanEdge(spaceName, part, edgeName, limit, startTime, endTime,
                DEFAULT_ALLOW_PART_SUCCESS, DEFAULT_ALLOW_READ_FOLLOWER);
    }


    /**
     * scan edge of all parts with return cols and limit, start time, end time, if allow partial
     * success, if allow read data from storage follower config.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName             Nebula space name
     * @param edgeName              Nebula edge type name
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           String edgeName,
                                           List<String> returnCols,
                                           int limit,
                                           long startTime,
                                           long endTime,
                                           boolean allowPartSuccess,
                                           boolean allowReadFromFollower) {

        List<Integer> parts = metaManager.getSpaceParts(spaceName);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("No valid part in space " + spaceName);
        }

        return scanEdge(spaceName, parts, edgeName, returnCols, false,
                limit, startTime, endTime, allowPartSuccess, allowReadFromFollower);
    }

    /**
     * scan edge of specific part with return cols and limit, start time, end time, if allow partial
     * success, if allow read data from storage follower config.
     *
     * <p>the result contains edge src id, dst id, rank and return cols.
     *
     * @param spaceName             Nebula space name
     * @param part                  Nebula data partition
     * @param edgeName              Nebula edge type name
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           int part,
                                           String edgeName,
                                           List<String> returnCols,
                                           int limit,
                                           long startTime,
                                           long endTime,
                                           boolean allowPartSuccess,
                                           boolean allowReadFromFollower) {
        return scanEdge(spaceName, Arrays.asList(part), edgeName, returnCols, false,
                limit, startTime, endTime, allowPartSuccess, allowReadFromFollower);
    }


    /**
     * scan edge of all parts with no return cols and limit, start time, end time, if allow partial
     * success, if allow read data from storage follower config.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName             Nebula space name
     * @param edgeName              Nebula edge type name
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           String edgeName,
                                           int limit,
                                           long startTime,
                                           long endTime,
                                           boolean allowPartSuccess,
                                           boolean allowReadFromFollower) {

        List<Integer> parts = metaManager.getSpaceParts(spaceName);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("No valid part in space " + spaceName);
        }
        return scanEdge(spaceName, parts, edgeName, new ArrayList<>(), true,
                limit, startTime, endTime, allowPartSuccess, allowReadFromFollower);
    }


    /**
     * scan edge of specific part with no return cols and limit, start time, end time, if allow
     * partial success, if allow read data from storage follower config.
     *
     * <p>the result only contains edge src id, dst id, rank.
     *
     * @param spaceName             Nebula space name
     * @param edgeName              Nebula edge type name
     * @param part                  Nebula data partition
     * @param limit                 the data amount of scan operation for each partition in every
     *                              iterator
     * @param startTime             the time range's start time of the data to be scanned, if
     *                              data was insert before start time, then it will not be return.
     * @param endTime               the time range's end time of the data to be scanned, if data
     *                              was insert after end time, then it will not be return.
     * @param allowPartSuccess      if allow part success
     * @param allowReadFromFollower if allow read from follower
     * @return an iterator to get data by call {@link ScanEdgeResultIterator#next()}
     */
    public ScanEdgeResultIterator scanEdge(String spaceName,
                                           int part,
                                           String edgeName,
                                           int limit,
                                           long startTime,
                                           long endTime,
                                           boolean allowPartSuccess,
                                           boolean allowReadFromFollower) {
        return scanEdge(spaceName, Arrays.asList(part), edgeName, new ArrayList<>(), true,
                limit, startTime, endTime, allowPartSuccess, allowReadFromFollower);
    }


    private ScanEdgeResultIterator scanEdge(String spaceName,
                                            List<Integer> parts,
                                            String edgeName,
                                            List<String> returnCols,
                                            boolean noColumns,
                                            int limit,
                                            long startTime,
                                            long endTime,
                                            boolean allowPartSuccess,
                                            boolean allowReadFromFollower) {
        if (spaceName == null || spaceName.trim().isEmpty()) {
            throw new IllegalArgumentException("space name is empty.");
        }
        if (edgeName == null || edgeName.trim().isEmpty()) {
            throw new IllegalArgumentException("edge name is empty");
        }
        if (noColumns && returnCols == null) {
            throw new IllegalArgumentException("returnCols is null");
        }

        Set<PartScanInfo> partScanInfoSet = new HashSet<>();
        for (int part : parts) {
            HostAddr leader = metaManager.getLeader(spaceName, part);
            partScanInfoSet.add(new PartScanInfo(part, new HostAddress(leader.getHost(),
                    leader.getPort())));
        }
        List<HostAddress> addrs = new ArrayList<>();
        for (HostAddr addr : metaManager.listHosts()) {
            addrs.add(new HostAddress(addr.getHost(), addr.getPort()));
        }
        List<byte[]> props = new ArrayList<>();
        props.add("_src".getBytes());
        props.add("_dst".getBytes());
        props.add("_rank".getBytes());
        if (!noColumns) {
            if (returnCols.size() == 0) {
                Schema schema = metaManager.getEdge(spaceName, edgeName).getSchema();
                for (ColumnDef columnDef : schema.getColumns()) {
                    props.add(columnDef.name);
                }
            } else {
                for (String prop : returnCols) {
                    props.add(prop.getBytes());
                }
            }
        }

        long edgeId = getEdgeId(spaceName, edgeName);
        EdgeProp edgeCols = new EdgeProp((int) edgeId, props);

        ScanEdgeRequest request = new ScanEdgeRequest();
        request
                .setSpace_id(getSpaceId(spaceName))
                .setReturn_columns(edgeCols)
                .setLimit(limit)
                .setStart_time(startTime)
                .setEnd_time(endTime)
                .setEnable_read_from_follower(allowReadFromFollower);

        return doScanEdge(spaceName, edgeName, partScanInfoSet, request, addrs, allowPartSuccess);
    }


    /**
     * do scan edge
     *
     * @param spaceName        Nebula graph space
     * @param edgeName         Nebula edge name
     * @param partScanInfoSet  leaders and scan cursors of all parts
     * @param request          {@link ScanVertexRequest}
     * @param addrs            storage server list
     * @param allowPartSuccess if allow partial success
     * @return result iterator
     */
    private ScanEdgeResultIterator doScanEdge(String spaceName,
                                              String edgeName,
                                              Set<PartScanInfo> partScanInfoSet,
                                              ScanEdgeRequest request,
                                              List<HostAddress> addrs,
                                              boolean allowPartSuccess) {
        if (addrs == null || addrs.isEmpty()) {
            throw new IllegalArgumentException("storage hosts is empty.");
        }

        return new ScanEdgeResultIterator.ScanEdgeResultBuilder()
                .withMetaClient(metaManager)
                .withPool(pool)
                .withPartScanInfo(partScanInfoSet)
                .withRequest(request)
                .withAddresses(addrs)
                .withSpaceName(spaceName)
                .withEdgeName(edgeName)
                .withPartSuccess(allowPartSuccess)
                .build();
    }


    /**
     * release storage client
     */
    public void close() {
        if (pool != null) {
            pool.close();
        }
        if (connection != null) {
            connection.close();
        }
    }


    /**
     * return client's connection session
     *
     * @return StorageConnection
     */
    protected GraphStorageConnection getConnection() {
        return this.connection;
    }


    /**
     * get the space id of specific space name
     *
     * @param spaceName Nebula space name
     * @return space id
     */
    private int getSpaceId(String spaceName) {
        return metaManager.getSpaceId(spaceName);
    }

    /**
     * get get edge id of specific edge type
     *
     * @param spaceName Nebula space name
     * @param edgeName  Nebula edge type name
     * @return edge id
     */
    private long getEdgeId(String spaceName, String edgeName) {
        return metaManager.getEdge(spaceName, edgeName).getEdge_type();
    }

    private static final int DEFAULT_LIMIT = 1000;
    private static final long DEFAULT_START_TIME = 0;
    private static final long DEFAULT_END_TIME = Long.MAX_VALUE;
    private static final boolean DEFAULT_ALLOW_PART_SUCCESS = false;
    private static final boolean DEFAULT_ALLOW_READ_FOLLOWER = true;
}
