/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperFWException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperExporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperExporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * This class exports data from Salt to the PAULA 1.0 format.
 * @author Mario Frank
 * @author Florian Zipser
 *
 */
@Component(name="PAULAExporterComponent", factory="PepperExporterComponentFactory")
//@Service(value=PepperExporter.class)
public class PAULAExporter extends PepperExporterImpl implements PepperExporter
{
	public PAULAExporter()
	{
		super();
		
		//setting name of module
		this.name= "PAULAExporter";
				
		//set list of formats supported by this module
		this.addSupportedFormat("paula", "1.0", null);
	}

	//===================================== start: thread number
	/**
	 * Defines the number of processes which can maximal work in parallel for importing documents.
	 * Means the number of parallel imported documents. Default value is 5.
	 */
	private Integer numOfParallelDocuments= 5;
	/**
	 * Sets the number of processes which can maximal work in parallel for importing documents.
	 * Means the number of parallel imported documents.
	 * @param numOfParallelDocuments the numOfParallelDocuments to set
	 */
	public void setNumOfParallelDocuments(Integer numOfParallelDocuments) {
		this.numOfParallelDocuments = numOfParallelDocuments;
	}

	/**
	 * Returns the number of processes which can maximal work in parallel for importing documents.
	 * Means the number of parallel imported documents.
	 * @return the numOfParallelDocuments
	 */
	public Integer getNumOfParallelDocuments() {
		return numOfParallelDocuments;
	}	
	
	public static final String PROP_VALIDATE_OUTPUT="paulaExporter.validateOutput";
	
	
	public static final String PROP_NUM_OF_PARALLEL_DOCUMENTS="paulaImporter.numOfParallelDocuments";
//===================================== start: thread number
	
// ========================== start: flagging for parallel running	
	/**
	 * If true, PAULAImporter imports documents in parallel.
	 */
	private Boolean RUN_IN_PARALLEL= true;
	/**
	 * @param rUN_IN_PARALLEL the rUN_IN_PARALLEL to set
	 */
	public void setRUN_IN_PARALLEL(Boolean rUN_IN_PARALLEL) {
		RUN_IN_PARALLEL = rUN_IN_PARALLEL;
	}

	/**
	 * @return the RUN_IN_PARALLEL
	 */
	public Boolean getRUN_IN_PARALLEL() {
		return RUN_IN_PARALLEL;
	}
	
	/**
	 * Identifier of properties which contains the maximal number of parallel processed documents. 
	 */
	public static final String PROP_RUN_IN_PARALLEL="paulaImporter.runInParallel";
// ========================== end: flagging for parallel running
	/**
	 * a property representation of a property file
	 */
	protected Properties props= null;
	
	
	/**
	 * Extracts properties out of given special parameters.
	 */
	private void exctractProperties()
	{
		this.props= new Properties();
		if (this.getSpecialParams()!= null)
		{//check if flag for running in parallel is set
			
			File propFile= new File(this.getSpecialParams().toFileString());
			//this.props= new Properties();
			InputStream in= null;
			try{
				in= new FileInputStream(propFile);
				this.props.load(in);
			}catch (Exception e)
			{
				throw new PAULAExporterException("Cannot find input file for properties: "+propFile+"\n nested exception: "+ e.getMessage());
			}
			finally
			{
				if (in != null)
				{
					try {
						in.close();
					} catch (IOException e) {
						throw new PAULAExporterException("Cannot close stream for file '"+props+"'. Nested exception is: "+ e);
					}
				}
			}
			if (this.props.containsKey(PROP_RUN_IN_PARALLEL))
			{
				try {
					Boolean val= new Boolean(this.props.getProperty(PROP_RUN_IN_PARALLEL));
					this.setRUN_IN_PARALLEL(val);
				} catch (Exception e) 
				{
					if (this.getLogService()!= null)
						this.getLogService().log(LogService.LOG_WARNING, "Cannot set correct property value of property "+PROP_RUN_IN_PARALLEL+" to "+this.getName()+", because of the value is not castable to Boolean. A correct value can contain 'true' or 'false'.");
				}
			}
			else if (this.props.containsKey(PROP_NUM_OF_PARALLEL_DOCUMENTS))
			{
				try {
					Integer val= new Integer(this.props.getProperty(PROP_NUM_OF_PARALLEL_DOCUMENTS));
					if (val > 0)
						this.setNumOfParallelDocuments(val);
				} catch (Exception e) 
				{
					if (this.getLogService()!= null)
						this.getLogService().log(LogService.LOG_WARNING, "Cannot set correct property value of property "+PROP_NUM_OF_PARALLEL_DOCUMENTS+" to "+this.getName()+", because of the value is not castable to Integer. A correct value must be a positiv, whole number (>0).");
				}
			}
		}//check if flag for running in parallel is set
	}
	
	/**
	 * Maps all sElementIds corresponding to SDocument object to the URI path were they shall be stored. 
	 */
	private Hashtable<SElementId, URI> sDocumentResourceTable= null;
	
	private void exportCorpusStructure()
	{
		if (this.getCorpusDefinition().getCorpusPath()== null)
			throw new PAULAExporterException("Cannot export SaltProject, because no corpus path is given.");
		if (this.getSaltProject()== null)
			throw new PAULAExporterException("Cannot export SaltProject, because it is null.");
		if (	(this.getSaltProject().getSCorpusGraphs()== null)||
				(this.getSaltProject().getSCorpusGraphs().size()== 0))
			throw new PAULAExporterException("Cannot export SaltProject, no SCorpusGraphs are given.");
		for (SCorpusGraph sCorpusGraph: this.getSaltProject().getSCorpusGraphs())
		{//walk through all corpus graphs, normally it is exactly one
			if (sCorpusGraph!= null)
			{
				Salt2PAULAMapper mapper= new Salt2PAULAMapper();
				Salt2PAULAMapper.setResourcePath(this.getResources());
				mapper.setPAULAExporter(this);
				mapper.setLogService(this.getLogService());
				if (props != null){
					String validate = props.getProperty(PROP_VALIDATE_OUTPUT, "no");
					if ("yes".equals(validate)){
						Salt2PAULAMapper.setValidating(true);
					}else{
						Salt2PAULAMapper.setValidating(false);
					}
				} else {
					Salt2PAULAMapper.setValidating(false);
				}
				
					
				sDocumentResourceTable= mapper.mapCorpusStructure(sCorpusGraph, this.getCorpusDefinition().getCorpusPath());
				if (	(sDocumentResourceTable== null)||
						(sDocumentResourceTable.size()== 0))
				{
					throw new PAULAExporterException("Cannot export SCorpusGraph '"+sCorpusGraph.getSName()+"', because of an unknown reason.");
				}
			}
		}//walk through all corpus graphs, normally it is exactly one
	}
	
	/**
	 * ThreadPool
	 */
	private ExecutorService executorService= null;
	
	@Override
	public void start()
	{
		{//extracts special parameters
			this.exctractProperties();
		}//extracts special parameters
		this.exportCorpusStructure();
		
		this.mapperRunners= new BasicEList<MapperRunner>();
		{//initialize ThreadPool
			executorService= Executors.newFixedThreadPool(this.getNumOfParallelDocuments());
		}//initialize ThreadPool
		
		
		
		boolean isStart= true;
		SElementId sElementId= null;
		while ((isStart) || (sElementId!= null))
		{	
			isStart= false;
			sElementId= this.getPepperModuleController().get();
			if (sElementId== null)
				break;
			
			//call for using push-method
			this.start(sElementId);
		}	
		
		for (MapperRunner mapperRunner: this.mapperRunners)
		{
			mapperRunner.waitUntilFinish();
		}
		this.end();
	}
	
	/**
	 * List of all used mapper runners.
	 */
	private EList<MapperRunner> mapperRunners= null;
	
	/**
	 * This method is called by method start() of superclass PepperImporter, if the method was not overriden
	 * by the current class. If this is not the case, this method will be called for every document which has
	 * to be processed.
	 * @param sElementId the id value for the current document or corpus to process  
	 */
	@Override
	public void start(SElementId sElementId) 
	{
		if (	(sElementId!= null) &&
				(sElementId.getSIdentifiableElement()!= null) &&
				((sElementId.getSIdentifiableElement() instanceof SDocument) ||
				((sElementId.getSIdentifiableElement() instanceof SCorpus))))
		{//only if given sElementId belongs to an object of type SDocument or SCorpus	
			if (sElementId.getSIdentifiableElement() instanceof SCorpus)
			{//mapping SCorpus	
			}//mapping SCorpus
			if (sElementId.getSIdentifiableElement() instanceof SDocument)
			{//mapping SDocument
				MapperRunner mapperRunner= new MapperRunner();
				{//configure mapper and mapper runner
					mapperRunner.mapper= new Salt2PAULAMapper();
					mapperRunner.sDocumentId= sElementId;
					mapperRunner.mapper.setLogService(this.getLogService());
				}//configure mapper and mapper runner
				
				if (this.getRUN_IN_PARALLEL())
				{//run import in parallel	
					this.mapperRunners.add(mapperRunner);
					this.executorService.execute(mapperRunner);
				}//run import in parallel
				else 
				{//do not run import in parallel
					mapperRunner.start();
				}//do not run import in parallel
			}//mapping SDocument
		}//only if given sElementId belongs to an object of type SDocument or SCorpus
	}
	
	/**
	 * This class is a container for running PAULAMappings in parallel.
	 * @author Administrator
	 *
	 */
	private class MapperRunner implements java.lang.Runnable
	{
		public SElementId sDocumentId= null;
		public Salt2PAULAMapper mapper= null;
		
		/**
		 * Lock to lock await and signal methods.
		 */
		protected Lock lock= new ReentrantLock();
		
		/**
		 * Flag wich says, if mapperRunner has started and finished
		 */
		private Boolean isFinished= false;
		
		/**
		 * If condition is achieved a new SDocument can be created.
		 */
		private Condition finishCondition=lock.newCondition();
		
		public void waitUntilFinish()
		{ 
			lock.lock();
			try {
				if (!isFinished)
					finishCondition.await();
			} catch (InterruptedException e) {
				throw new PepperFWException(e.getMessage());
			}
			lock.unlock();
		}
		
		@Override
		public void run() 
		{
			start();
		}
		
		/**
		 * starts Mapping of PAULA data
		 */
		public void start()
		{
			if (mapper== null)
				throw new PAULAExporterException("BUG: Cannot start export, because the mapper is null.");
			if (sDocumentId== null)
				throw new PAULAExporterException("BUG: Cannot start export, because no SDocument object is given.");
			URI paulaDoc= null;
			{//getting paula-document-path
				paulaDoc= sDocumentResourceTable.get(sDocumentId);
				if (paulaDoc== null)
					throw new PAULAExporterException("BUG: Cannot start export, no paula-document-path was found for SDocument '"+sDocumentId+"'.");
			}//getting paula-document-path
			try 
			{
				mapper.mapSDocumentStructure((SDocument)sDocumentId.getSIdentifiableElement(), paulaDoc);
				getPepperModuleController().put(sDocumentId);
			}catch (Exception e)
			{
				e.printStackTrace();
				if (getLogService()!= null)
				{
					getLogService().log(LogService.LOG_WARNING, "Cannot export the SDocument '"+sDocumentId+"'. The reason is: "+e);
				}
				getPepperModuleController().finish(sDocumentId);
			}
			this.lock.lock();
			this.isFinished= true;
			this.finishCondition.signal();
			this.lock.unlock();
		}
	}
}
