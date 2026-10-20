package fr.gambitdrame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/**
 * Original scalable wood relief chess set (no enclosing circular medallions).
 * flat: a longitudinal half-piece resting on its broad flat side, seen from above.
 * perspective: fuller carved relief with a raised side / offset wooden extrusion.
 * All six silhouettes are independent of Unicode font glyph availability.
 */
public final class WoodPieces {
    private WoodPieces() {}

    public static void draw(Canvas board,char piece,float cx,float cy,float cell,boolean perspective){
        boolean light=Character.isUpperCase(piece);
        char kind=Character.toUpperCase(piece);
        Canvas c=board;
        c.save();
        c.translate(cx-cell*.46f,cy-cell*.46f);
        c.scale(cell*.0092f,cell*.0092f);
        if(perspective){ c.translate(2,-2); c.skew(-.08f,0); }
        Path outline=shape(kind);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        // Contact shadow stays inside the piece footprint, not a round backing.
        p.setColor(0x55000000);
        c.save();c.translate(3.5f,5);c.drawPath(outline,p);c.restore();
        if(perspective){
            p.setColor(light?0xff9b6b3b:0xff180d08);
            c.save();c.translate(0,6);c.drawPath(outline,p);c.restore();
        }else{
            // A gentle cut-edge on the half-piece: raised side, flat back.
            p.setColor(light?0xff9c7649:0xff24120b);
            c.save();c.translate(1.7f,2.8f);c.drawPath(outline,p);c.restore();
        }
        int[] grain=light?
            new int[]{0xff86613d,0xffcfaa6c,0xfff7e1ad,0xffbc905a,0xfff3d49a}:
            new int[]{0xff150c08,0xff4b2c1a,0xff866040,0xff392016,0xff705034};
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(12,0,90,0,grain,null,Shader.TileMode.CLAMP));
        c.drawPath(outline,p);
        p.setShader(null);
        // Subtle dark silhouette edge for strong accessibility contrast.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(light?2.7f:2.3f);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(light?0xff3e2c1c:0xffebc895);
        c.drawPath(outline,p);
        p.setStyle(Paint.Style.FILL);
        // Wood grain follows the piece's longitudinal cut, clipped to silhouette.
        c.save(); c.clipPath(outline);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(perspective?1.1f:1.4f);
        p.setColor(light?0x65967446:0x747d5938);
        for(int i=0;i<7;i++){
            float gx=21+i*10;
            Path stria=new Path();
            stria.moveTo(gx,3);
            stria.cubicTo(gx-6,34,gx+7,57,gx-3,104);
            c.drawPath(stria,p);
        }
        p.setStrokeWidth(perspective?2.2f:3.0f);
        p.setColor(light?0x8dfff3cf:0x7abda37e);
        Path highlight=new Path();
        highlight.moveTo(32,28);highlight.quadTo(40,55,35,82);
        c.drawPath(highlight,p);
        c.restore();
        // Hand-carved recognizable motifs for king, bishop and knight.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.4f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(light?0xff6d4929:0xffddbb82);
        if(kind=='B'){
            c.drawLine(43,22,58,48,p);
        }else if(kind=='K'){
            c.drawLine(49,10,49,29,p);c.drawLine(41,17,57,17,p);
        }else if(kind=='N'){
            c.drawCircle(37,38,2.7f,p);
            c.drawLine(51,29,62,39,p);
        }else if(kind=='Q'){
            c.drawLine(29,37,71,37,p);
        }
        c.restore();
    }

    private static Path shape(char kind){
        Path path=new Path();
        switch(kind){
            case 'P':
                path.addCircle(50,25,15,Path.Direction.CW);
                path.moveTo(42,38);path.lineTo(58,38);path.lineTo(65,68);
                path.lineTo(74,78);path.lineTo(74,86);path.lineTo(26,86);
                path.lineTo(26,78);path.lineTo(35,68);path.close();
                break;
            case 'R':
                path.moveTo(24,15);path.lineTo(37,15);path.lineTo(37,25);
                path.lineTo(44,25);path.lineTo(44,15);path.lineTo(56,15);
                path.lineTo(56,25);path.lineTo(63,25);path.lineTo(63,15);
                path.lineTo(76,15);path.lineTo(73,36);path.lineTo(66,42);
                path.lineTo(64,70);path.lineTo(74,79);path.lineTo(74,87);
                path.lineTo(26,87);path.lineTo(26,79);path.lineTo(36,70);
                path.lineTo(34,42);path.lineTo(27,36);path.close();
                break;
            case 'N':
                path.moveTo(31,88);path.lineTo(31,79);path.lineTo(40,68);
                path.lineTo(40,61);path.lineTo(24,51);path.lineTo(18,44);
                path.lineTo(27,35);path.lineTo(32,15);path.lineTo(39,20);
                path.lineTo(45,14);path.lineTo(50,25);path.lineTo(65,29);
                path.lineTo(75,41);path.lineTo(69,58);path.lineTo(60,62);
                path.lineTo(66,79);path.lineTo(73,79);path.lineTo(73,88);
                path.close();break;
            case 'B':
                path.moveTo(50,12);path.cubicTo(28,26,29,46,45,55);
                path.lineTo(55,55);path.cubicTo(73,42,68,26,50,12);
                path.close();
                path.moveTo(42,53);path.lineTo(58,53);path.lineTo(64,70);
                path.lineTo(73,80);path.lineTo(73,88);path.lineTo(27,88);
                path.lineTo(27,80);path.lineTo(36,70);path.close();break;
            case 'Q':
                path.moveTo(19,24);path.lineTo(30,39);path.lineTo(35,18);
                path.lineTo(45,35);path.lineTo(50,12);path.lineTo(55,35);
                path.lineTo(65,18);path.lineTo(70,39);path.lineTo(81,24);
                path.lineTo(68,59);path.lineTo(65,72);path.lineTo(75,80);
                path.lineTo(75,88);path.lineTo(25,88);path.lineTo(25,80);
                path.lineTo(35,72);path.lineTo(32,59);path.close();break;
            case 'K':
                path.moveTo(46,8);path.lineTo(54,8);path.lineTo(54,16);
                path.lineTo(63,16);path.lineTo(63,24);path.lineTo(54,24);
                path.lineTo(54,31);path.lineTo(69,39);path.lineTo(65,69);
                path.lineTo(75,80);path.lineTo(75,88);path.lineTo(25,88);
                path.lineTo(25,80);path.lineTo(35,69);path.lineTo(31,39);
                path.lineTo(46,31);path.lineTo(46,24);path.lineTo(37,24);
                path.lineTo(37,16);path.lineTo(46,16);path.close();break;
            default:
                path.addCircle(50,48,22,Path.Direction.CW);
        }
        return path;
    }
}
