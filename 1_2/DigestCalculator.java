import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class DigestCalculator {

	public byte[] getFileContentInByteArray ( String filePath ) {
		Path path = Paths.get(filePath);
		byte[] content = null;
		try {
			content = Files.readAllBytes(path);
		} catch ( IOException e ) {
			System.err.println("[ERROR-FILE CONTENT RETRIEVE] Problem to read file in path: " + filePath);
			System.exit(1);
		}
		return content;
	}

	public Map<Integer, byte[]> calculateFilesDigests ( List<String> filesList, String digestType ) {

		Map<Integer, byte[]> filesDigests = new HashMap<Integer, byte[]>();
		MessageDigest messageDigest = null;
		try{
			messageDigest = MessageDigest.getInstance(digestType);
		}  catch ( NoSuchAlgorithmException e) {
			System.err.println("[ERROR-DIGEST CALCULATION] Non-existing algorithm for MessageDigest: " + digestType);
			System.exit(1);
		}

		for(int i = 0; i < filesList.size(); i++) {
			messageDigest.update(this.getFileContentInByteArray(filesList.get(i)));
			filesDigests.put(i, messageDigest.digest());
		}

		return filesDigests;
	}

	public List<String> compareFilesDigests (List<String> filesList, Map<Integer, byte[]> filesDigests, String digestType) {

		List<String> results = new ArrayList<>();
		List<Integer> collisionsIndexes = null;

		// Search for collisions
		for(int i = 0; (i < filesList.size()) && (i < filesDigests.size()); i++) { // To compare
			// If it already is in collision, doesn't makes sense to compare with the rest because it happened in a previous iteration
			if(collisionsIndexes != null && collisionsIndexes.contains(new Integer(i)))
				continue;

			for(int j = i + 1; (j < filesList.size()) && (j < filesDigests.size()); j++) { // To compare with
				// If it already is in collision, doesn't makes sense to compare with the rest because it happened in a previous iteration
				if(collisionsIndexes != null && collisionsIndexes.contains(new Integer(j)))
					continue;

				if(digestsAreEqual(filesDigests.get(i), filesDigests.get(j))) { // collision
					if(collisionsIndexes == null)
						collisionsIndexes = new ArrayList<>();
					if(!collisionsIndexes.contains(new Integer(i))) {
						collisionsIndexes.add(i);
						results.add(filesList.get(i).substring(filesList.get(i).lastIndexOf("\\")).replace("\\", "") + " " +
									digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " COLISION");
					}
					if(!collisionsIndexes.contains(new Integer(j))) {
						collisionsIndexes.add(j);
						results.add(filesList.get(j).substring(filesList.get(j).lastIndexOf("\\")).replace("\\", "") + " " +
									digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(j)) + " COLISION");
					}		
				}
			}	
		}

		// Eliminate those indexes where collision happened (not gonna use them anymore)
		if(collisionsIndexes != null) {
			Collections.sort(collisionsIndexes); // To avoid indexing problems when removing
			for(int i = collisionsIndexes.size() - 1; i >= 0; i--) {
				int removed = collisionsIndexes.get(i); // Had to pass to a variable, so it's not recognized as Object parameter in step below...
				filesList.remove(removed);
				for(int j = collisionsIndexes.get(i); j < filesDigests.size(); j++) {
					if(j < filesDigests.size() - 1)
						filesDigests.replace(j, filesDigests.get(j + 1));
					else
						filesDigests.remove(j);
				}
			}
		}

		return results;
	}

	public void compareFilesWithDigestListFile (List<String> filesList, Map<Integer, byte[]> filesDigests, String digestType, List<String> dlfAsList, List<String> results, String digestListFile) {

		boolean dlfChanged = false;
		int listSize = dlfAsList.size(); // list size before possible changes, to avoid unnecessary comparisons between program arguments...

		for(int i = 0; i < filesList.size(); i++) { // for each argument
			String fileName = filesList.get(i).substring(filesList.get(i).lastIndexOf("\\")).replace("\\", "");
				
			if(listSize == 0) {
				dlfChanged = true;
			
				dlfAsList.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)));
				results.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " NOT FOUND");
			}
			else {
				int fileIndex = -1; // for when if collisions didn't happen until the end and file is in the list
				String[] fileSplittedDLFRow = null;

				for(int j = 0; j < listSize; j++) { // for each file inside list, before any changes
					String[] splittedDLFRow = dlfAsList.get(j).split(" ");

					if(fileName.equals(splittedDLFRow[0])) { // found file
						fileIndex = j;
						fileSplittedDLFRow = splittedDLFRow;
					}
					else if((digestType.equals(splittedDLFRow[1]) && DatatypeConverter.printHexBinary(filesDigests.get(i)).equals(splittedDLFRow[2])) ||
							(splittedDLFRow.length == 5 && digestType.equals(splittedDLFRow[3]) && DatatypeConverter.printHexBinary(filesDigests.get(i)).equals(splittedDLFRow[4]))) { // collision
							
						results.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " COLISION");
						break;
					}

					if(j == listSize - 1) { // no collisions, check file on list
						if(fileIndex > -1) {
							if((digestType.equals(fileSplittedDLFRow[1]) && DatatypeConverter.printHexBinary(filesDigests.get(i)).equals(fileSplittedDLFRow[2])) ||
								(fileSplittedDLFRow.length == 5 && digestType.equals(fileSplittedDLFRow[3]) && DatatypeConverter.printHexBinary(filesDigests.get(i)).equals(fileSplittedDLFRow[4]))) { // digest type and digests check
							
								results.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " OK");
							}
							else if((digestType.equals(fileSplittedDLFRow[1]) && !DatatypeConverter.printHexBinary(filesDigests.get(i)).equals(fileSplittedDLFRow[2])) ||
									(fileSplittedDLFRow.length == 5 && digestType.equals(fileSplittedDLFRow[3]) && !DatatypeConverter.printHexBinary(filesDigests.get(i)).equals(fileSplittedDLFRow[4]))) { // digest type check, digests doesn't
							
								results.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " NOT OK");
							}
							else { // digest type not there
								dlfChanged = true;

								dlfAsList.set(fileIndex, dlfAsList.get(fileIndex).concat(" " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i))));
								results.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " NOT FOUND");
							}
						}
						else {
							dlfChanged = true;
						
							dlfAsList.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)));
							results.add(fileName + " " + digestType + " " + DatatypeConverter.printHexBinary(filesDigests.get(i)) + " NOT FOUND");
						}
					}
				}
			}
		}

		// Writting into DigestListFile
		if(dlfChanged) {
			String dlfContent = dlfAsList.get(0) + "\n";
			for(int i = 1; i < dlfAsList.size(); i++) {
				dlfContent = dlfContent.concat(dlfAsList.get(i) + "\n");
			}
			updateDigestListFile(digestListFile, dlfContent.getBytes());
		}
	}

	private boolean digestsAreEqual ( byte[] digestOne, byte[] digestTwo ) {

		for(int i = 0; (i < digestOne.length) && (i < digestTwo.length); i++)
			if(digestOne[i] != digestTwo[i])
				return false;
		return true;
	}

	private void updateDigestListFile ( String digestListFilePath, byte[] dlfContent ) {

		File dlFile = new File(digestListFilePath);
		FileOutputStream fos = null;
		try {
			Files.deleteIfExists(dlFile.toPath());
			dlFile.getParentFile().mkdirs();
			dlFile.createNewFile();

			fos = new FileOutputStream(digestListFilePath);
			fos.write(dlfContent);
			fos.close();
		} catch ( IOException e ) {
			System.err.println("[ERROR-DIGEST LIST FILE REWRITE] Problem to deal with DigestListFile update" + "\nERROR MESSAGE: " + e.getCause().getMessage());
			System.exit(1);
		}
	}

	public static void main (String[] args) throws Exception {

		// Testing for missing input
		if(args.length < 3) {
			System.err.println("Missing input!\nProgram usage: java DigestCalculator \"type digest\" \"DigestListFile path\" \"File1 path\" \"File2 path\" ... \"FileN path\"");
			System.exit(1);
		}

		String digestType = args[0];
		//String digestType = "SHA1";

		//String digestListFile = args[1];
		String digestListFile = "C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\DigestListFile.txt";
		
		List<String> filesList = new ArrayList<>();
		//for(int i = 2; i <= args.length-1; i++)
		//	filesList.add(args[i]);
		//for(int i = 0; i < filesList.size(); i++)
		//	System.out.println(filesList.get(i));
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\DigestListFile.txt");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\MySignature - Copia.java");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\MySignature.java");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\MySignature.class");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\TrabLab.pdf");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\cam.bat");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\MySignature - Copia (2).java");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\DigestCalculator.class");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\TrabLab - Copia.pdf");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\MySignature - Copia.class");
		filesList.add("C:\\Users\\NinoFabrizio\\Desktop\\Trab1 e 2\\MySignature - Copia (2).java");
	
		DigestCalculator dc = new DigestCalculator();

		// Retrieving DigestListFile's content
		byte[] dlfContent = dc.getFileContentInByteArray(digestListFile);
		System.out.println(digestListFile.substring(digestListFile.lastIndexOf("\\")).replace(".txt", "").replace("\\", "") + "'s content before:");
		List<String> dlfAsList = new ArrayList<String>(Arrays.asList(new String(dlfContent, "UTF-8").split("\n")));

		if(dlfAsList.get(0).isEmpty()) // to avoid empty String getting included in DigestListFile
			dlfAsList.clear();

		for(int i = 0; i < dlfAsList.size(); i++)
			System.out.println(dlfAsList.get(i));
		System.out.println("");

		// Calculating files' digests
		// Obs.: Key: file's index in filesList (Integer) - Value: digest (byte[])
		Map<Integer, byte[]> filesDigests = dc.calculateFilesDigests(filesList, digestType);
		//for(int i = 0; i < filesList.size(); i++)
		//	System.out.println(filesList.get(i).substring(filesList.get(i).lastIndexOf("\\")).replace("\\", "") + "'s digest:\n" + DatatypeConverter.printHexBinary(filesDigests.get(i)));
	
		// Comparing digests between arguments
		List<String> results = dc.compareFilesDigests(filesList, filesDigests, digestType);
		//System.out.println("Results:");
		//for(int i = 0; i < results.size(); i++)
		//	System.out.println(results.get(i));
		//System.out.println("");
		//for(int i = 0; i < filesList.size(); i++)
		//	System.out.println(filesList.get(i));
		//for(int i = 0; i < filesList.size(); i++)
		//	System.out.println(filesList.get(i).substring(filesList.get(i).lastIndexOf("\\")).replace("\\", "") + "'s digest:\n" + DatatypeConverter.printHexBinary(filesDigests.get(i)));

		// Comparing files digests with DigestListFile's content
		if(!filesList.isEmpty()) // All file arguments could have collided
			dc.compareFilesWithDigestListFile(filesList, filesDigests, digestType, dlfAsList, results, digestListFile);
		System.out.println(digestListFile.substring(digestListFile.lastIndexOf("\\")).replace(".txt", "").replace("\\", "") + "'s content after:");
		for(int i = 0; i < dlfAsList.size(); i++)
			System.out.println(dlfAsList.get(i));
		System.out.println("");

		// Showing final results
		System.out.println("Results:");
		for(int i = 0; i < results.size(); i++)
			System.out.println(results.get(i));
	}
}