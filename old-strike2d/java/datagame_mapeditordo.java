package skayrux.shootout2d;

public class datagame_mapeditordo {
    public byte td;
    public short texind;
    public byte svp;
    public short x0, y0, x1, y1;
    public byte obtip;
    public short obid;
    public boolean otmen;

    public datagame_mapeditordo(int td, int texind, int svp, float x0, float y0, float x1, float y1){
        this.td=(byte)td;
        this.texind=(short)texind;
        this.svp=(byte)svp;
        this.x0=(short)x0;
        this.y0=(short)y0;
        this.x1=(short)x1;
        this.y1=(short)y1;
        this.otmen=false;
    }

    public datagame_mapeditordo(int td, int obtip, float x, float y, int obid){
        this.td=(byte)td;
        this.obtip=(byte)obtip;
        this.x0=(short)x;
        this.y0=(short)y;
        this.obid=(short)obid;
        this.otmen=false;
    }
}
