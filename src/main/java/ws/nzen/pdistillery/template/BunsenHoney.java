
/** see ../../../../../../../LICENSE for release rights */
package ws.nzen.pdistillery.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Changes relevant yaml with html to yaml with more html */
public class BunsenHoney
{
	private static final String cl = "o.";
	static final String templateConfigFlag = "t", yamlConfigFlag = "y",
			filesToProcessFlag = "f", verboseFlag = "v",
			outputFolderFlag = "o", helpFlag = "h";
	static final String pluginKey = "pd_left", selfAsPlugin = "bunsen-h";
	protected boolean verbose;
	protected Path templateConfig = null;
	protected Path yamlConfig = null;
	protected Path outputFolder = null;
	protected YamlConfig docConfig = null;

	protected enum PathType
	{
		YAML( false ),
		TEMPLATE( true ),
		OUTPUT( true );

		private final boolean targetIsDir;

		PathType( boolean amDirectory )
		{
			targetIsDir = amDirectory;
		}

		boolean shouldBeDirectory()
		{
			return targetIsDir;
		}
	};


	/** read configs, interpret template, copy to output folder */
	public static void main( String[] args )
	{
		/*
		carve out the md specific stuff
		180115 next:
		add hardcoded template
		*/
		CommandLine userInput = prepCli( prepCliParser(), args );
		BunsenHoney doesStuff = prepDoer( userInput );
		if ( userInput != null )
		{
			doesStuff.wrapInTemplate( userInput
					.getOptionValues( filesToProcessFlag ) );
		}
	}


	/** fills options with our cli flags and text */
	public static Options prepCliParser()
	{
		Options knowsCliDtd = new Options();
		final boolean needsEmbelishment = true;
		knowsCliDtd.addOption( templateConfigFlag, needsEmbelishment, "template config path"
				+ " (ex C:\\Program Files\\apache\\tomcat.txt)" );
		knowsCliDtd.addOption( yamlConfigFlag, needsEmbelishment, "yaml config path"
				+ " (ex /home/theusername/tmp/ff.json)" );
		knowsCliDtd.addOption( outputFolderFlag, needsEmbelishment,
				"path of folder to write results" );
		knowsCliDtd.addOption( filesToProcessFlag, needsEmbelishment,
				"paths of files to process" );
		knowsCliDtd.addOption( verboseFlag, ! needsEmbelishment,
				"show debug information" );
		knowsCliDtd.addOption( helpFlag, "show arg flags" );
		final int numberOfFilesToAcceptFromFolder
				= org.apache.commons.cli.Option.UNLIMITED_VALUES;
		knowsCliDtd.getOption( filesToProcessFlag )
				.setArgs( numberOfFilesToAcceptFromFolder );
		return knowsCliDtd;
	}


	/** Parses the actual input and shows help, if requested */
	public static CommandLine prepCli(
			Options knowsCliDtd, String[] args )
	{
		CommandLineParser cliRegex = new DefaultParser();
		CommandLine userInput = null;
		try
		{
			userInput = cliRegex.parse( knowsCliDtd, args );
			if ( userInput.hasOption( helpFlag ) )
			{
				new HelpFormatter().printHelp( "PD-BunsenHoney", knowsCliDtd );
			}
		}
		catch ( ParseException pe )
		{
			System.err.println( cl +"pc just using config: couldn't parse input "+ pe );
		}
		return userInput;
	}


	public static BunsenHoney prepDoer( CommandLine userInput )
	{
		BunsenHoney doesStuff;
		if ( userInput != null && userInput.hasOption( verboseFlag ) )
		 {
			boolean wantsLogging = true;
			doesStuff = new BunsenHoney( wantsLogging );
		 }
		else
		{
			 doesStuff = new BunsenHoney();
		}
		if ( userInput != null )
		{
			final String currentDir = "";
			if ( userInput.hasOption( templateConfigFlag ) )
			{
				doesStuff.setPath( userInput.getOptionValue(
						templateConfigFlag, currentDir ), PathType.TEMPLATE,
						"template folder" );
			}
			if ( userInput.hasOption( yamlConfigFlag ) )
			{
				doesStuff.setPath( userInput.getOptionValue(
						yamlConfigFlag, currentDir ), PathType.YAML,
						"yaml config" );
			}
			doesStuff.setPath( userInput.getOptionValue(
					outputFolderFlag, currentDir ), PathType.OUTPUT,
					"output folder" );
		}
		return doesStuff;
	}


	public BunsenHoney()
	{
		this( false );
	}


	public BunsenHoney( boolean noiseTolerance )
	{
		verbose = noiseTolerance;
	}


	public void setPath( String path, PathType which,
			String descOfWanted )
	{
		final String here = cl +"sp ";
		try
		{
			Path place = Paths.get( path );
			boolean appropriateFileType = (which.shouldBeDirectory())
					? place.toFile().isDirectory()
					: place.toFile().isFile();
			if ( appropriateFileType )
			{
				if ( which == PathType.YAML )
				{
					templateConfig = place;
				}
				else if ( which == PathType.TEMPLATE )
				{
					yamlConfig = place;
				}
				else if ( which == PathType.OUTPUT )
				{
					outputFolder = place;
				}
			}
			else if ( verbose )
			{
				System.err.println( here + descOfWanted +" must be a file" );
			}
		}
		catch ( InvalidPathException ipe )
		{
			System.err.println( here +"invalid "+
					descOfWanted +" path "+ ipe );
		}
	}


	/** cleanup after config changes */
	public void adoptConfiguration()
	{
		System.out.println( cl +"ac didnt reify config yet" );
		if ( docConfig == null )
		{
			docConfig = new YamlConfig();
		}
		// configure yaml thing from actual config
		docConfig.writeConfig.setExplicitFirstDocument( true );
		docConfig.writeConfig.setExplicitEndDocument( true );
		// configure template thing from actual config
	}


	/** wrap html of the yaml files with template specified if appropriate */
	public void wrapInTemplate( String[] paths )
	{
		final String here = cl +"wit ";
		if ( paths != null )
		{
			adoptConfiguration();
			YamlReader loadsInfo;
			YamlWriter burysInfo;
			for ( String path : paths )
			{
				System.out.println( cl +"tmo trans "+ path );
				try ( FileReader charLoader = new FileReader( path );
						FileWriter charDump = new FileWriter(
							outputFolder.toString() + File.separator
							+ Paths.get( path ).getFileName() ) )
				{
					if ( verbose )
					{
						System.out.println( here +"new file is "
								+ outputFolder.toString() + File.separator
								+ Paths.get( path ).getFileName() );
					}
					
					loadsInfo = new YamlReader( charLoader );
					burysInfo = new YamlWriter( charDump );
					Object document;
					Map docAttributes;
					// assert doc 1 is a map, doc 2 is a scalar literal
					document = loadsInfo.read();
					if ( document == null || ! (document instanceof Map) )
					{
						loadsInfo.close();
						copyFileWithoutChange(new FileReader( path ), charDump);
						continue;
					}
					else
					{
						if ( verbose )
						{
							System.out.println( document );
						}
						docAttributes = (Map)document;
						if ( docAttributes.containsKey( pluginKey ) )
						{
							List plugins = (List)(docAttributes.get( pluginKey ));
							// IMPROVE handle class cast ex
							int indOfSelf = plugins.indexOf( selfAsPlugin );
							if ( indOfSelf >= 0 )
							{
								plugins.remove( indOfSelf );
								docAttributes.put( pluginKey, plugins );
								burysInfo.write( document );
							}
							else
							{
								loadsInfo.close();
								copyFileWithoutChange(new FileReader( path ), charDump);
								continue;
							}
						}
						else
						{
							loadsInfo.close();
							copyFileWithoutChange(new FileReader( path ), charDump);
							continue;
						}
					}
					document = loadsInfo.read();
					if ( document == null )
					{
						loadsInfo.close();
						burysInfo.close();
						continue;
					}
					else if ( ! (document instanceof String) )
					{
						do
						{
							document = loadsInfo.read();
							burysInfo.write( document );
						}
						while ( document != null );
						loadsInfo.close();
						burysInfo.close();
						continue;
					}
					else
					{
						// this is our content to wrap
						String content = (String)document;
						content = content.toUpperCase();
						burysInfo.write( content );
					}
					document = loadsInfo.read();
					if ( document == null )
					{
						loadsInfo.close();
						burysInfo.close();
						continue;
					}
					else
					{
						do
						{
							document = loadsInfo.read();
							burysInfo.write( document );
						}
						while ( document != null );
						loadsInfo.close();
						burysInfo.close();
						continue;
					}
				}
				catch ( FileNotFoundException | InvalidPathException ie )
				{
					System.err.println( here +"invalid path "+ ie );
				}
				catch ( IOException ie )
				{
					System.err.println( here +"file i/o problem "+ ie );
				}
			}
		}
	}


	protected void copyFileWithoutChange( FileReader original,
			FileWriter duplicate ) throws IOException
	{
		int maybeByte;
		maybeByte = original.read();
		while ( maybeByte != -1 )
		{
			duplicate.write( maybeByte );
			maybeByte = original.read();
		}
	}

}




























