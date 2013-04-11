/*-------------------------------------------------------------------------
*
* hd_work_mgr.h
*	  distributes hdfs file splits or hbase table regions for processing 
*     between GP segments
*
* Copyright (c) 2007-2008, Greenplum inc
*
*-------------------------------------------------------------------------
*/
#ifndef HDWORKMGR_H
#define HDWORKMGR_H

#include "c.h"
#include "utils/rel.h"
#include "nodes/pg_list.h"
#include "lib/stringinfo.h"

char** map_hddata_2gp_segments(char *uri, int num_segs);
void free_hddata_2gp_segments(char **segs_work_map, int num_segs);

/*
 * Structure that describes one Statistics element received from the GPXF service
 */
typedef struct sGpxfStatsElem
{
	int   blockSize; /* size of a block size in the GPXF target datasource */
	int   numBlocks;
	int   numTuples;
} GpxfStatsElem;
GpxfStatsElem *get_gpxf_statistics(char *uri, Relation rel, StringInfo err_msg);

#endif   /* HDWORKMGR_H */
