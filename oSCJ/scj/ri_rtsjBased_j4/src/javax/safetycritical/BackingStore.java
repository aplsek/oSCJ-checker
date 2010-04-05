package javax.safetycritical;

class BackingStore extends MissionMemory {

    BackingStore(long sizeInByte) {
        super(sizeInByte);
    }

    void setManager(MissionManager mngr) {
        setPortal(mngr);
    }

    public MissionManager getManager() {
        return (MissionManager) getPortal();
    }
}
