/*
** A2Enhanced.java add a circle that the mouse is the center of the circle when drawing shapes
*/

package com.company;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.ArrayList;


// View interface
interface IView {
    public void updateView();
}


// simple shape model class
class Shape {

    // shape points
    ArrayList<Point2d> points;
    int midx;
    int midy;

    public int getMidx() {return midx;}
    public int getMidy() {return midy;}

    public void clearPoints() {
        points = new ArrayList<Point2d>();
        pointsChanged = true;
    }

    // add a point to end of shape
    public void addPoint(Point2d p) {
        if (points == null) clearPoints();
        points.add(p);
        pointsChanged = true;
    }

    // add a point to end of shape
    public void addPoint(double x, double y) {
        addPoint(new Point2d(x, y));
    }

    public int npoints() {
        return points.size();
    }

    // shape is polyline or polygon
    Boolean isClosed = false;

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean isClosed) {
        this.isClosed = isClosed;
    }

    // if polygon is filled or not
    Boolean isFilled = false;

    public Boolean getIsFilled() {
        return isFilled;
    }

    public void setIsFilled(Boolean isFilled) {
        this.isFilled = isFilled;
    }

    // drawing attributes
    Color colour = Color.BLACK;
    float strokeThickness = 2.0f;

    public Color getColour() {
        return colour;
    }

    public void setColour(Color colour) {
        this.colour = colour;
    }

    public float getStrokeThickness() {
        return strokeThickness;
    }

    public void setStrokeThickness(float strokeThickness) {
        this.strokeThickness = strokeThickness;
    }

    // shape's transform

    // quick hack, get and set would be better
    float scale = 1.0f;
    int angle = 0;
    int translatedx = 0;
    int translatedy = 0;
    int translatingx = 0;
    int translatingy = 0;
    public float getScale() {return scale;}
    public void setScale(float flt) { scale = flt; }
    public int getAngle() {return angle;}
    public void setAngle(int angle) { this.angle = angle; }
    public void setTranslatingX(int x) { translatingx = x; }
    public void setTranslatingY(int y) { translatingy = y; }
    public void setTranslatedX() {
        translatedx += translatingx;
        translatingx = 0;
    }
    public void setTranslatedY() {
        translatedy += translatingy;
        translatingy = 0;
    }
    public int getTranslateX() {
        return translatingx + translatedx;
    }

    public int getTranslateY() {
        return translatingy + translatedy;
    }

    // some optimization to cache points for drawing
    Boolean pointsChanged = false; // dirty bit
    int[] xpoints, ypoints;
    int npoints = 0;

    void cachePointsArray() {
        int sumy = 0;
        int sumx = 0;
        xpoints = new int[points.size()];
        ypoints = new int[points.size()];
        for (int i=0; i < points.size(); i++) {
            xpoints[i] = (int)points.get(i).x;
            ypoints[i] = (int)points.get(i).y;
            sumx += (int)points.get(i).x;
            sumy += (int)points.get(i).y;
        }
        midx = sumx /xpoints.length;
        midy = sumy / ypoints.length;
        npoints = points.size();
        pointsChanged = false;
    }
    public ArrayList<Point2d> getPoints() {
        return points;
    }

    // let the shape draw itself
    // (note this isn't good separation of shape View from shape Model)
    public void draw(Graphics2D g2) {

        // don't draw if points are empty (not shape)
        if (points == null) return;

        // see if we need to update the cache
        if (pointsChanged) cachePointsArray();

        // save the current g2 transform matrix
        AffineTransform M = g2.getTransform();

        // multiply in this shape's transform
        // (uniform scale)
        AffineTransform S = AffineTransform.getScaleInstance(scale, scale);
        AffineTransform R = AffineTransform.getRotateInstance(Math.toRadians(angle));
        AffineTransform T = AffineTransform.getTranslateInstance(translatedx + translatingx,translatedy + translatingy);

        g2.translate(midx, midy);
        g2.transform(T);
        g2.transform(R);
        g2.transform(S);
        g2.translate(-midx, -midy);


        // call drawing functions
        g2.setColor(colour);
        // can adjust stroke size using scale
        g2.setStroke(new BasicStroke(strokeThickness / scale));
        g2.drawPolyline(xpoints, ypoints, npoints);
        // reset the transform to what it was before we drew the shape
        g2.setTransform(M);
    }
}


class DrawingModel {

    // the data in the model, a list of  Shape
    private ArrayList<Shape> p_ShapeList = new ArrayList<Shape>();
    private Shape p_HighLightShape;

    private int downmost = 0;
    private int rightmost = 0;

    // all views of this model
    private ArrayList<IView> views = new ArrayList<IView>();

    // set the view observer
    public void addView(IView view) {
        views.add(view);
        // update the view to current state of the model
        view.updateView();
    }

    // notify the IView observer
    private void notifyObservers() {
        for (IView view : this.views) {
            view.updateView();
        }
    }

    public ArrayList<Shape> getShapes() {
        return p_ShapeList;
    }

    public Shape getHighLightShape() {
        return p_HighLightShape;
    }

    public void setHighlightNull() {
        p_HighLightShape = null;
        notifyObservers();
    }

    public void flush() {
        notifyObservers();
    }

    public Shape shapeInclude(int x, int y) {
        for (int i = p_ShapeList.size()-1;i>=0;i--) {
            Shape shape = p_ShapeList.get(i);

            double cos = Math.cos(Math.toRadians(shape.getAngle()));
            double sin = Math.sin(Math.toRadians(shape.getAngle()));
            int centerx = x - shape.getMidx() - shape.getTranslateX();
            int centery = y - shape.getMidy() - shape.getTranslateY();
            int xx = (int)(centerx/shape.getScale()*cos + centery/shape.getScale()*sin + shape.getMidx());
            int yy = (int)(-centerx/shape.getScale()*sin + centery / shape.getScale()*cos + shape.getMidy());

            if (shape.getPoints() != null) {
                for (Point2d point : shape.getPoints()) {
                    if (Math.abs(point.x - xx) <= 3 && Math.abs(point.y - yy) <= 3) {
                        return shape;
                    }
                }
            }
        }
        return null;
    }

    public void highlight (int x, int y) {
        Shape shape = shapeInclude(x,y);
        p_HighLightShape = shape;
        notifyObservers();
    }

    public int FindLeftmost() {
        rightmost = 0;
        for (Shape shape : p_ShapeList) {
            if (shape.getPoints() != null) {
                double cos = Math.cos(Math.toRadians(shape.getAngle()));
                double sin = Math.sin(Math.toRadians(shape.getAngle()));

                for (Point2d point : shape.getPoints()) {
                    int x = (int) (((point.x - shape.getMidx()) * cos - (point.y - shape.getMidy()) * sin) * shape.getScale()
                            + shape.getTranslateX() + shape.getMidx());
                    if (x > rightmost) {
                        rightmost = x;
                    }
                }
            }
        }
        return rightmost;
    }

    public int FindDownmost() {
        downmost = 0;
        for (Shape shape : p_ShapeList) {
            if (shape.getPoints() != null) {
                double cos = Math.cos(Math.toRadians(shape.getAngle()));
                double sin = Math.sin(Math.toRadians(shape.getAngle()));

                for (Point2d point : shape.getPoints()) {
                    int y = (int) (((point.x - shape.getMidx()) * sin + (point.y - shape.getMidy()) * cos) * shape.getScale()
                            + shape.getTranslateY() + shape.getMidy());
                    if (y > downmost) {
                        downmost = y;
                    }
                }
            }
        }
        return downmost;
    }
}


class ToolbarView extends JPanel {

    private JButton p_Button;
    private JSlider p_ScaleSlider;
    private JLabel p_ScaleLabel;
    private JSlider p_RotateSlider;
    private JLabel p_RotateLabel;

    private Shape p_LastHighlight;
    // the model that this view is showing
    private DrawingModel model;

    public ToolbarView(DrawingModel model) {
        this.model = model;

        this.setLayout(new FlowLayout(FlowLayout.LEFT,20,10));
        this.setBorder(BorderFactory.createLineBorder(Color.black));

        p_Button = new JButton("Delete");
        p_ScaleSlider = new JSlider(50,200,100);
        p_ScaleLabel = new JLabel("1.0");
        p_RotateSlider = new JSlider(-180,180,0);
        p_RotateLabel = new JLabel("0");

        p_Button.setEnabled(false);
        p_ScaleSlider.setEnabled(false);
        p_RotateSlider.setEnabled(false);

        this.add(p_Button);
        this.add(p_ScaleSlider);
        this.add(p_ScaleLabel);
        this.add(p_RotateSlider);
        this.add(p_RotateLabel);

        this.model.addView(new IView() {
            public void updateView() {
                if (model.getHighLightShape() == null) {
                    p_Button.setEnabled(false);
                    p_ScaleSlider.setEnabled(false);
                    p_RotateSlider.setEnabled(false);
                } else {
                    p_Button.setEnabled(true);
                    p_ScaleSlider.setEnabled(true);
                    p_RotateSlider.setEnabled(true);
                }
                if (model.getHighLightShape() != p_LastHighlight && model.getHighLightShape() != null) {
                    p_ScaleSlider.setValue((int)(model.getHighLightShape().getScale() * 100));
                    DecimalFormat numberFormat = new DecimalFormat("0.0");
                    p_ScaleLabel.setText(numberFormat.format((float) p_ScaleSlider.getValue() / 100));

                    p_RotateSlider.setValue(model.getHighLightShape().getAngle());
                    p_RotateLabel.setText(Integer.toString(p_RotateSlider.getValue()));

                    p_LastHighlight = model.getHighLightShape();
                }
            }
        });

        p_ScaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider s = (JSlider) e.getSource();
                DecimalFormat numberFormat = new DecimalFormat("0.0");
                p_ScaleLabel.setText(numberFormat.format((float) s.getValue() / 100));

                if (model.getHighLightShape() != null && model.getHighLightShape() == p_LastHighlight) {
                    model.getHighLightShape().setScale((float) s.getValue() / 100);
                }
                model.flush();
            }
        });

        p_RotateSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider s = (JSlider)e.getSource();
                p_RotateLabel.setText(Integer.toString(s.getValue()));

                if (model.getHighLightShape() != null && model.getHighLightShape() == p_LastHighlight) {
                    model.getHighLightShape().setAngle(s.getValue());
                }
                model.flush();
            }
        });

        p_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.getShapes().remove(model.getHighLightShape());
                model.setHighlightNull();
            }
        });
    }
}


class StatusbarView extends JPanel {
    private JLabel p_InfoLabel;

    // the model that this view is showing
    private DrawingModel model;

    public StatusbarView (DrawingModel model) {
        this.model = model;
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setBorder(BorderFactory.createLineBorder(Color.black));
        p_InfoLabel = new JLabel("");

        this.add(p_InfoLabel);

        this.model.addView(new IView() {
            public void updateView() {
                String text = "";
                if (model.getShapes().size() == 1) {
                    text += Integer.toString(model.getShapes().size()) + " Stroke";
                } else {
                    text += Integer.toString(model.getShapes().size()) + " Strokes";
                }
                if (model.getHighLightShape() != null) {
                    DecimalFormat numberFormat = new DecimalFormat("0.0");
                    text += ", Selection (" + Integer.toString(model.getHighLightShape().getPoints().size()) +
                            " points, scale: " + numberFormat.format(model.getHighLightShape().getScale()) +
                            ", rotation " + Integer.toString(model.getHighLightShape().getAngle()) + ")";
                }
                p_InfoLabel.setText(text);
            }
        });
    }
}


class CanvasView extends JPanel {
    private DrawingModel model;
    private Shape shape;
    private boolean Isdrag = false;
    Point2d pressedPoint = new Point2d(0,0);
    Point2d draggedPoint = new Point2d(-1,-1);

    public CanvasView(DrawingModel model) {
        this.model = model;

        this.setBackground(Color.WHITE);

        this.model.addView(new IView() {
            public void updateView() {
                setPreferredSize(new Dimension(model.FindLeftmost(),
                        model.FindDownmost()));
                revalidate();
                repaint();
            }
        });

        this.addMouseListener (new MouseAdapter(){
            public void mouseClicked(MouseEvent e) {
                model.highlight(e.getX(), e.getY());
            }
        });

        this.addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e) {
                shape = new Shape();
                shape.scale = 1.0f;
                model.getShapes().add(shape);
                pressedPoint.x = e.getX();
                pressedPoint.y = e.getY();
            }
        });

        this.addMouseListener(new MouseAdapter(){
            public void mouseReleased(MouseEvent e) {
                if (Isdrag) {
                    if (model.getHighLightShape() != null) {
                        model.getHighLightShape().setTranslatedX();
                        model.getHighLightShape().setTranslatedY();
                        model.getShapes().remove(model.getShapes().size()-1);
                    }
                    Isdrag = false;
                } else {
                    model.getShapes().remove(model.getShapes().size()-1);
                }
                model.flush();
            }
        });

        this.addMouseMotionListener(new MouseAdapter(){
            public void mouseDragged(MouseEvent e) {
                if (model.getHighLightShape() != null) {
                    model.getHighLightShape().setTranslatingX((int)(e.getX() - pressedPoint.x));
                    model.getHighLightShape().setTranslatingY((int)(e.getY()-pressedPoint.y));
                } else {
                    draggedPoint.x = e.getX();
                    draggedPoint.y = e.getY();
                    shape.addPoint(e.getX(), e.getY());
                }
                Isdrag = true;
                repaint();
            }
        });

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g; // cast to get 2D drawing methods
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  // antialiasing look nicer
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (Isdrag) {
            if(draggedPoint.x !=-1 && draggedPoint.y != -1) {
                g2.drawOval((int) draggedPoint.x - 15, (int) draggedPoint.y - 15, 30, 30);
            }
            draggedPoint.x = -1;
            draggedPoint.y = -1;
        }

        Shape highLightShape = model.getHighLightShape();
        if (highLightShape != null) {
            highLightShape.setStrokeThickness(6.0f);
            highLightShape.setColour(Color.YELLOW);
            highLightShape.draw(g2);
        }

        for (Shape tshape : model.getShapes()) {
            if (tshape != null) {
                tshape.setStrokeThickness(2.0f);
                tshape.setColour(Color.BLACK);
                tshape.draw(g2);
            }
        }
    }
}


class A2Enhanced {

    public static void main(String[] args){
        JFrame frame = new JFrame("A2Basic");

        // create DrawingModel and initialize it
        DrawingModel drawingmodel = new DrawingModel();

        // create ToolbarView, tell it about model
        ToolbarView toolbarView = new ToolbarView(drawingmodel);

        // create StatusbarView
        StatusbarView statusbarView = new StatusbarView(drawingmodel);

        // create CanvasView
        CanvasView canvasView = new CanvasView(drawingmodel);

        JPanel panel = new JPanel(new BorderLayout());
        frame.getContentPane().add(panel);

        JScrollPane scrollPane = new JScrollPane(canvasView,ScrollPaneConstants.
                VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(toolbarView,BorderLayout.NORTH);
        panel.add(scrollPane,BorderLayout.CENTER);
        panel.add(statusbarView,BorderLayout.SOUTH);

        // setup window
        frame.setPreferredSize(new Dimension(900,600));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
