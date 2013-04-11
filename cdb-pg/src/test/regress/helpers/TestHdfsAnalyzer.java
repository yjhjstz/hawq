import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;

import com.emc.greenplum.gpdb.hdfsconnector.BaseAnalyzer;
import com.emc.greenplum.gpdb.hdfsconnector.BaseMetaData;
import com.emc.greenplum.gpdb.hdfsconnector.DataSourceStatsInfo;
import com.emc.greenplum.gpdb.hdfsconnector.GPFusionInputFormat;
import com.emc.greenplum.gpdb.hdfsconnector.GPHdfsBridge;
import com.emc.greenplum.gpdb.hdfsconnector.HDFSMetaData;
import com.emc.greenplum.gpdb.hdfsconnector.HDMetaData;
import com.emc.greenplum.gpdb.hdfsconnector.IHdfsFileAccessor;


/*
 * Analyzer class for HDFS data resources
 *
 * Given an HDFS data source (a file, directory, or wild card pattern)
 * return statistics about it (number of blocks, number of tuples, etc.)
 */
public class TestHdfsAnalyzer extends BaseAnalyzer
{	
	private	JobConf jobConf;
	private FileSystem fs;
	private DataSourceStatsInfo stats;
	private Log Log;
	
	/*
	 * C'tor
	 */
	public TestHdfsAnalyzer(BaseMetaData md) throws IOException
	{
		super(md);
		Log = LogFactory.getLog(TestHdfsAnalyzer.class);

		jobConf = new JobConf(new Configuration(), TestHdfsAnalyzer.class);
		fs = FileSystem.get(jobConf);
	}
	
	/*
	 * path is a data source URI that can appear as a file 
	 * name, a directory name  or a wildcard returns the data 
	 * fragments in json format
	 */	
	public String GetEstimatedStats(String datapath) throws Exception
	{
		long blockSize = 0;
		long numberOfBlocks = 0;
		
		Path	path = new Path("/" + datapath); //yikes! any better way?
		long numberOfTuplesInBlock = getNumberOfTuplesInBlock(); 
		
		InputSplit[] splits = getSplits(path);
		
		for (InputSplit split : splits)
		{	
			FileSplit fsp = (FileSplit)split;
			
			if (blockSize == 0) // blockSize wasn't updated yet
			{
				Path filePath = fsp.getPath();
				FileStatus fileStatus = fs.getFileStatus(filePath);
				if (fileStatus.isFile()) {
					blockSize = fileStatus.getBlockSize();
					break;
				}
			}
		}
	
		// if no file is in path (only dirs), get default block size
		if (blockSize == 0)
			blockSize = fs.getDefaultBlockSize();
		numberOfBlocks = splits.length;
		
		stats = new DataSourceStatsInfo(blockSize, numberOfBlocks, numberOfTuplesInBlock*numberOfBlocks);
		
		//print files size to log when in debug level
		Log.debug(DataSourceStatsInfo.dataToString(stats, path.toString()));

		return DataSourceStatsInfo.dataToJSON(stats);
	}
	
	/*
	 * Calculate the number of tuples in a split (block) 
	 * Reads one block from HDFS. Exception during reading will
	 * filter upwards and handled in AnalyzerResource
	 */
	private long getNumberOfTuplesInBlock() throws Exception
	{
		long tuples = -1; /* default  - if we are not able to read data */
		
		HDFSMetaData hdfsConf = new HDFSMetaData(new HDMetaData(conf.getParametersMap()));
		IHdfsFileAccessor accessor = GPHdfsBridge.getFileAccessor(hdfsConf);		
		if (accessor.Open());
		{
			tuples = 0;
			while (accessor.LoadNextObject() != null)
				tuples++;

			accessor.Close();
		}
		
		return tuples;
	}	
	
	private InputSplit[] getSplits(Path path) throws IOException 
	{
		GPFusionInputFormat fformat = new GPFusionInputFormat();
		GPFusionInputFormat.setInputPaths(jobConf, path);
		return fformat.getSplits(jobConf, 1);
	}
	
}