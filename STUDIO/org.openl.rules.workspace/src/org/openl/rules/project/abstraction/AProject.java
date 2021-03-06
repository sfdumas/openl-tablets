package org.openl.rules.project.abstraction;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

public class AProject extends AProjectFolder {
    /**
     * true if the project has a folder structure and false if the project is stored as a zip
     */
    private boolean folderStructure;
    protected List<FileData> historyFileDatas;
    private String lastHistoryVersion;

    public AProject(Repository repository, String folderPath, boolean folderStructure) {
        this(repository, folderPath, null, folderStructure);
    }

    public AProject(Repository repository, String folderPath, String historyVersion, boolean folderStructure) {
        super(null, repository, folderPath, historyVersion);
        this.folderStructure = folderStructure;
    }

    public AProject(Repository repository, FileData fileData, boolean folderStructure) {
        super(null, repository, fileData.getName(), fileData.getVersion());
        this.folderStructure = folderStructure;
        setFileData(fileData);
    }

    @Override
    public FileData getFileData() {
        FileData fileData = super.getFileData();
        if (fileData == null) {
            if (!isFolder()) {
                try {
                if (!isHistoric() || isLastVersion()) {
                    fileData = getRepository().check(getFolderPath());
                    if (fileData == null) {
                        fileData = new LazyFileData(getFolderPath(), getHistoryVersion(), this);
                    }
                } else {
                    fileData = getRepository().checkHistory(getFolderPath(), getHistoryVersion());
                }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            } else {
                fileData = new LazyFileData(getFolderPath(), getHistoryVersion(), this);
            }
            setFileData(fileData);
        }
        return fileData;
    }

    protected String getLastHistoryVersion() {
        if (lastHistoryVersion == null) {
            List<FileData> fileDatas = getHistoryFileDatas();
            lastHistoryVersion = fileDatas.isEmpty() ? null : fileDatas.get(fileDatas.size() - 1).getVersion();
        }
        return lastHistoryVersion;
    }

    final protected void setLastHistoryVersion(String lastHistoryVersion) {
        this.lastHistoryVersion = lastHistoryVersion;
    }

    @Override
    public ProjectVersion getLastVersion() {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(fileDatas.size() - 1));
    }

    protected boolean isLastVersion() {
        String historyVersion = getHistoryVersion();
        if (historyVersion == null) {
            return true;
        }
        String lastHistoryVersion = getLastHistoryVersion();
        return lastHistoryVersion == null || historyVersion.equals(lastHistoryVersion);    }

    @Override
    public List<ProjectVersion> getVersions() {
        Collection<FileData> fileDatas = getHistoryFileDatas();
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    @Override
    public int getVersionsCount() {
        return getHistoryFileDatas().size();
    }

    @Override
    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    protected List<FileData> getHistoryFileDatas() {
        if (historyFileDatas == null) {
            try {
                String folderPath = getFolderPath();
                if (folderPath != null && !isFolder()) {
                    historyFileDatas = getRepository().listHistory(folderPath);
                } else {
                    // Local repository doesn't have versions
                    historyFileDatas = Collections.emptyList();
                }
            } catch (IOException ex) {
                throw RuntimeExceptionWrapper.wrap(ex);
            }
        }
        return historyFileDatas;
    }

    @Override
    public int getFirstRevisionIndex() {
        List<FileData> fileDatas = getHistoryFileDatas();
        if (fileDatas.isEmpty()) {
            return 0;
        }

        // In JCR repository first revision is "technical". It doesn't contain any file and comment.
        // In this case first real revision is 1, not 0.
        // In other situations it's impossible to create empty project in repository.
        // In other repository types first revision is always 0.
        FileData fd = fileDatas.get(0);
        boolean technicalRevision = fd.getSize() == 0 && StringUtils.isEmpty(fd.getComment());
        return technicalRevision ? 1 : 0;
    }

    @Override
    public void setFileData(FileData fileData) {
        super.setFileData(fileData);
        historyFileDatas = null;
    }

    @Override
    public AProject getProject() {
        return this;
    }

    @Override
    public void delete() throws ProjectException {
        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                artefact.delete();
            }
        } else {
            unlock();
            close(null);
            FileData fileData = getFileData();
            if (!getRepository().delete(fileData)) {
                throw new ProjectException("Project is absent or can't be deleted");
            }
        }
        setFileData(null);
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isDeleted()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        unlock();
        close(user);
        FileData fileData = getFileData();
        if (!getRepository().delete(fileData)) {
            throw new ProjectException("Resource is absent or can't be deleted");
        }
        setFileData(null);
    }

    public void save(CommonUser user) throws ProjectException {
        if (!isFolder()) {
            FileData fileData = getFileData();
            FileItem read;
            InputStream stream = null;
            try {
            if (isHistoric()) {
                read = getRepository().readHistory(fileData.getName(), fileData.getVersion());
            } else {
                read = getRepository().read(fileData.getName());
            }
            stream = read.getStream();
            fileData.setSize(read.getData().getSize());
            setFileData(getRepository().save(fileData, stream));
            } catch (IOException ex) {
                throw new ProjectException("Project cannot be saved", ex);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        refresh();
    }

    public void close(CommonUser user) throws ProjectException {
        refresh();
    }

    public void erase() throws ProjectException {
        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    if (!getRepository().deleteHistory(artefact.getFileData().getName(), null)) {
                        throw new ProjectException("Can't erase project because it is absent or can't be deleted");
                    }
                }
            }
        } else {
            if (!getRepository().deleteHistory(getFileData().getName(), null)) {
                throw new ProjectException("Can't erase project because it is absent or can't be deleted");
            }
        }
    }

    public boolean isDeleted() {
        if (isFolder()) {
            Collection<AProjectArtefact> artefacts = getArtefacts();
            if (artefacts.isEmpty()) {
                // Projects can be empty but not deleted. For example revision #0.
                return false;
            }
            for (AProjectArtefact artefact : artefacts) {
                if (artefact instanceof AProjectResource) {
                    if (!artefact.getFileData().isDeleted()) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return getFileData().isDeleted();
        }
    }

    public void undelete() throws ProjectException {
        try {
            if (!isDeleted()) {
                throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
            }

            Repository repository = getRepository();
            if (isFolder()) {
                for (AProjectArtefact artefact : getArtefacts()) {
                    if (artefact instanceof AProjectResource) {
                        FileData fileData = repository.check(artefact.getFileData().getName());
                        if (fileData != null && fileData.isDeleted()) {
                            repository.deleteHistory(fileData.getName(), fileData.getVersion());
                            FileData actual = repository.check(fileData.getName());
                            artefact.setFileData(actual);
                        }
                    }
                }
            } else {
                FileData fileData = repository.check(getFileData().getName());
                if (fileData != null && fileData.isDeleted()) {
                    repository.deleteHistory(fileData.getName(), fileData.getVersion());
                    FileData actual = repository.check(fileData.getName());
                    setFileData(actual);
                    String version = actual.getVersion();
                    setLastHistoryVersion(version);
                    setHistoryVersion(version);
                }
            }
        } catch (IOException ex) {
            throw new ProjectException("Cannot undelete a project", ex);
        }

    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String path = artefactPath.getStringValue();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        AProjectArtefact artefact = getArtefactsInternal().get(path);
        if (artefact == null) {
            // For backward compatibility throw exception if artefact isn't found
            throw new ProjectException("Cannot find project artefact ''{0}''", null, path);
        }
        return artefact;
    }

    @Override
    protected Map<String, AProjectArtefact> createInternalArtefacts() {
        if (isFolder()) {
            return super.createInternalArtefacts();
        }

        final HashMap<String, AProjectArtefact> internalArtefacts = new HashMap<String, AProjectArtefact>();

        final String folderPath = getFolderPath();
        final Repository repository = getRepository();
        FileItem fileItem;
        try {
        if (isHistoric()) {
            fileItem = repository.readHistory(folderPath, getFileData().getVersion());
        } else {
            fileItem = repository.read(folderPath);
        }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        if (fileItem == null) {
            return internalArtefacts;
        }
        ZipInputStream zipInputStream = new ZipInputStream(fileItem.getStream());
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                FileData fileData = new FileData();
                final String artefactName = entry.getName();
                fileData.setName(folderPath + "/" + artefactName);
                String version = isHistoric() ? getFileData().getVersion() : null;
                ZipFolderRepository zipFolderRepository = new ZipFolderRepository(repository, folderPath, version);
                AProjectResource resource = new AProjectResource(getProject(), zipFolderRepository, fileData);
                internalArtefacts.put(artefactName, resource);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }

        return internalArtefacts;
    }

    public boolean isOpenedForEditing() {
        // TODO Remove this workaround
        return false;
    }

    @Override
    public void update(AProjectArtefact newFolder, CommonUser user) throws ProjectException {
        if (!(newFolder instanceof AProject)) {
            throw new IllegalArgumentException("Can't update not from AProject");
        }

        AProject projectFrom = (AProject) newFolder;

        if (isFolder()) {
            if (projectFrom.isFolder()) {
                super.update(projectFrom, user);
            } else {
                ZipInputStream stream = null;
                try {
                    if (projectFrom.isHistoric()) {
                        FileItem fileItem = projectFrom.getRepository().readHistory(projectFrom.getFolderPath(), projectFrom.getFileData().getVersion());
                        if (fileItem == null) {
                            return;
                        }
                        stream = new ZipInputStream(fileItem.getStream());
                    } else {
                        FileItem fileItem = projectFrom.getRepository().read(projectFrom.getFolderPath());
                        if (fileItem == null) {
                            return;
                        }
                        stream = new ZipInputStream(fileItem.getStream());
                    }
                    String folderPath = getFolderPath();

                    ZipEntry entry;
                    while ((entry = stream.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }
                        FileData fileData = new FileData();
                        fileData.setName(folderPath + "/" + entry.getName());
                        fileData.setSize(entry.getSize());
                        fileData.setModifiedAt(new Date(entry.getTime()));
                        getRepository().save(fileData, stream);
                    }
                } catch (IOException e) {
                    throw new ProjectException("Can't update: " + e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        } else {
            if (!projectFrom.isFolder()) {
                if (getResourceTransformer() != null) {
                    // projectFrom will be unarchived, transformed and then archived
                    transformAndArchive(projectFrom, user);
                } else {
                    // Just copy a single file
                    FileData fileData = getFileData();

                    InputStream stream = null;
                    try {
                        if (projectFrom.isHistoric()) {
                            FileItem fileItem = projectFrom.getRepository().readHistory(projectFrom.getFolderPath(), projectFrom.getFileData().getVersion());
                            fileData.setSize(fileItem.getData().getSize());
                            stream = fileItem.getStream();
                        } else {
                            FileItem fileItem = projectFrom.getRepository().read(projectFrom.getFolderPath());
                            fileData.setSize(fileItem.getData().getSize());
                            stream = fileItem.getStream();
                        }
                        fileData.setAuthor(user.getUserName());
                        setFileData(getRepository().save(fileData, stream));
                    } catch (IOException ex) {
                        throw new ProjectException("Can't update: " + ex.getMessage(), ex);
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            } else {
                transformAndArchive(projectFrom, user);
            }
        }
    }

    private void transformAndArchive(AProject projectFrom, CommonUser user) throws ProjectException {
        // Archive the folder using zip
        FileData fileData = getFileData();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(out);

            for (AProjectArtefact artefact : projectFrom.getArtefacts()) {
                writeArtefact(zipOutputStream, artefact);
            }

            fileData.setAuthor(user.getUserName());
            fileData.setSize(out.size());
            setFileData(getRepository().save(fileData, new ByteArrayInputStream(out.toByteArray())));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    private void writeArtefact(ZipOutputStream zipOutputStream, AProjectArtefact artefact) throws
                                                                                           IOException,
                                                                                           ProjectException {
        if ((artefact instanceof AProjectResource)) {
            AProjectResource resource = (AProjectResource) artefact;
            zipOutputStream.putNextEntry(new ZipEntry(resource.getInternalPath()));

            ResourceTransformer transformer = getResourceTransformer();
            InputStream content = transformer != null ? transformer.transform(resource) : resource.getContent();
            IOUtils.copy(content, zipOutputStream);

            content.close();
            zipOutputStream.closeEntry();
        } else {
            AProjectFolder folder = (AProjectFolder) artefact;
            for (AProjectArtefact a : folder.getArtefacts()) {
                writeArtefact(zipOutputStream, a);
            }
        }
    }

    @Override
    public boolean isFolder() {
        return folderStructure;
    }

    protected void setFolderStructure(boolean folderStructure) {
        this.folderStructure = folderStructure;
    }

    @Override
    public String getInternalPath() {
        // The root of the project
        return "";
    }

    @Override
    public boolean hasArtefacts() {
        return isFolder() ? super.hasArtefacts() : getFileData().getSize() != 0;
    }

    private static class LazyFileData extends FileData {
        private AProject project;

        private LazyFileData(String name, String version, AProject project) {
            setName(name);
            setVersion(version == null ? project.getLastHistoryVersion() : version);
            this.project = project;
        }

        @Override
        public long getSize() {
            verifyInitialized();
            return super.getSize();
        }

        @Override
        public void setSize(long size) {
            verifyInitialized();
            super.setSize(size);
        }

        @Override
        public String getAuthor() {
            verifyInitialized();
            return super.getAuthor();
        }

        @Override
        public void setAuthor(String author) {
            verifyInitialized();
            super.setAuthor(author);
        }

        @Override
        public String getComment() {
            verifyInitialized();
            return super.getComment();
        }

        @Override
        public void setComment(String comment) {
            verifyInitialized();
            super.setComment(comment);
        }

        @Override
        public Date getModifiedAt() {
            verifyInitialized();
            return super.getModifiedAt();
        }

        @Override
        public void setModifiedAt(Date modifiedAt) {
            verifyInitialized();
            super.setModifiedAt(modifiedAt);
        }

        @Override
        public boolean isDeleted() {
            verifyInitialized();
            return super.isDeleted();
        }

        @Override
        public void setDeleted(boolean deleted) {
            verifyInitialized();
            super.setDeleted(deleted);
        }

        private void verifyInitialized() {
            if (project != null) {
                List<FileData> fileDatas = project.getHistoryFileDatas();
                if (!fileDatas.isEmpty()) {
                    FileData repoData = null;

                    String version = getVersion();
                    if (version == null) {
                        repoData = fileDatas.get(fileDatas.size() - 1);
                    } else {
                        for (FileData data : fileDatas) {
                            if (data.getVersion().equals(version)) {
                                repoData = data;
                                break;
                            }
                        }
                    }

                    if (repoData != null) {
                        super.setAuthor(repoData.getAuthor());
                        super.setModifiedAt(repoData.getModifiedAt());
                        super.setComment(repoData.getComment());
                        super.setSize(repoData.getSize());
                        super.setDeleted(repoData.isDeleted());
                    }
                }
                project = null;
            }
        }
    }
}
