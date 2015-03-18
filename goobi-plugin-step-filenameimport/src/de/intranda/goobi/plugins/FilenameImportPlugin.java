package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;
import de.intranda.goobi.plugins.utils.Image;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;

@PluginImplementation
public class FilenameImportPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {

    private static final String PLUGIN_NAME = "FilenameImportPlugin";
    private static final Logger logger = Logger.getLogger(FilenameImportPlugin.class);

    private List<Image> imageList = new ArrayList<Image>();

    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean execute() {
        Process process = myStep.getProzess();

        // read files
        String imageFolderName = "";
        try {
            imageFolderName = process.getImagesTifDirectory(false);
        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            logger.error(e);
            return false;
        }
        File folder = new File(imageFolderName);
        String[] files = folder.list(FileFileFilter.FILE);

        List<String> filenames = Arrays.asList(files);
        Collections.sort(filenames);
        for (String filename : filenames) {
            if (filename.endsWith("pdf")) {
                // move to ocr folder
                try {
                    String destination = process.getPdfDirectory();
                    File destinationFolder = new File(destination);
                    if (!destinationFolder.exists()) {
                        destinationFolder.mkdirs();
                    }
                    FileUtils.copyFileToDirectory(new File(folder, filename), destinationFolder);
                } catch (SwapException | DAOException | IOException | InterruptedException e) {
                    logger.error(e);
                    Helper.setFehlerMeldung(e);
                    return false;
                }
            } else if (filename.endsWith("xml")) {
                // move to alto folder
                try {
                    String destination = process.getAltoDirectory();
                    File destinationFolder = new File(destination);
                    if (!destinationFolder.exists()) {
                        destinationFolder.mkdirs();
                    }
                    FileUtils.copyFileToDirectory(new File(folder, filename), destinationFolder);
                } catch (SwapException | DAOException | IOException | InterruptedException e) {
                    logger.error(e);
                    Helper.setFehlerMeldung(e);
                    return false;
                }
            } else if (filename.equals("Thumbs.db")) {
                // delete
                FileUtils.deleteQuietly(new File(folder, filename));
            } else {
                // import as page
                String[] parts = filename.replace(".tif", "").split("-");
                //                String identifier = parts[0];
                String physicalPageNo = parts[1].replace("S", "");
                String docstructType = parts[2];
                String logicalPageNo = parts[3];

                Image img = new Image(logicalPageNo, physicalPageNo, docstructType);
                imageList.add(img);
            }
        }

        return importImageData(process);
    }

    private boolean importImageData(Process process) {
        try {

            Prefs prefs = process.getRegelsatz().getPreferences();
            DocStructType pageType = prefs.getDocStrctTypeByName("page");
            MetadataType physType = prefs.getMetadataTypeByName("physPageNumber");
            MetadataType logType = prefs.getMetadataTypeByName("logicalPageNumber");

            Fileformat fileformat = process.readMetadataFile();
            DigitalDocument digitalDocument = fileformat.getDigitalDocument();
            DocStruct logical = digitalDocument.getLogicalDocStruct();
            if (logical.getType().isAnchor()) {
                logical = logical.getAllChildren().get(0);
            }
            DocStruct physical = digitalDocument.getPhysicalDocStruct();

            // cleanup previous imports

            cleanupPages(digitalDocument, physical);
            cleanupDocstructs(logical);

            DocStruct latest = null;
            for (Image img : imageList) {
                DocStruct page = digitalDocument.createDocStruct(pageType);

                Metadata phys = new Metadata(physType);
                phys.setValue(img.getPhysicalNo());
                page.addMetadata(phys);

                Metadata log = new Metadata(logType);
                log.setValue(img.getLogicalNo());
                page.addMetadata(log);

                physical.addChild(page);

                // create new logical docstruct
                if (latest == null || !latest.getType().getName().equals(img.getDocstruct())) {
                    latest = digitalDocument.createDocStruct(prefs.getDocStrctTypeByName(img.getDocstruct()));
                    logical.addChild(latest);
                }

                // reference between physical, logical and latest element
                logical.addReferenceTo(page, "logical_physical");
                latest.addReferenceTo(page, "logical_physical");
            }

            process.writeMetadataFile(fileformat);
            return true;
        } catch (ReadException | PreferencesException | SwapException | DAOException | WriteException | IOException | InterruptedException e1) {
            logger.error(e1);
            Helper.setFehlerMeldung(e1);
        } catch (TypeNotAllowedForParentException | MetadataTypeNotAllowedException | TypeNotAllowedAsChildException e) {
            logger.error(e);
            Helper.setFehlerMeldung(e);
        }
        return false;
    }

    private void cleanupPages(DigitalDocument digitalDocument, DocStruct physical) {
        List<DocStruct> pages = physical.getAllChildren();
        if (pages != null) {
            for (DocStruct page : pages) {
                digitalDocument.getFileSet().removeFile(page.getAllContentFiles().get(0));

                List<Reference> refs = new ArrayList<Reference>(page.getAllFromReferences());
                for (ugh.dl.Reference ref : refs) {
                    ref.getSource().removeReferenceTo(page);
                }
            }
        }
        while (physical.getAllChildren() != null && !physical.getAllChildren().isEmpty()) {
            physical.removeChild(physical.getAllChildren().get(0));
        }
    }

    private void cleanupDocstructs(DocStruct logical) {

        while (logical.getAllChildren() != null && !logical.getAllChildren().isEmpty()) {
            logical.removeChild(logical.getAllChildren().get(0));
        }
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

}
