package skayrux.shootout2d;

public class datagame_mapeditorobject {
    public byte obtip;
    public String name;
    public int maxcount;

    public datagame_mapeditorobject(int obtip, String name, int maxcount) {
        this.obtip = (byte) obtip;
        this.name = name;
        this.maxcount = maxcount;
    }
}
