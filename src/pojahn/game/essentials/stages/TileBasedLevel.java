package pojahn.game.essentials.stages;


import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import pojahn.game.core.Entity;
import pojahn.game.core.Level;
import pojahn.game.essentials.Image2D;

public abstract class TileBasedLevel extends Level{
	
	private int tilesX, tilesY, tileWidth, tileHeight;
	private float rotation;
	private Entity worldImage;
	private TiledMap map;
	private TiledMapRenderer tiledMapRenderer;
	private TiledMapTileLayer layer;
	private OrthographicCamera camera;
	private MapProperties props;
	private Map<Integer, Byte> tileLayer;
	private Map<Integer, Holder> orgTiles;
	private Image2D tileSet;
	
	protected TileBasedLevel(){
		tileLayer = new HashMap<>();
		orgTiles = new HashMap<>();
	}
	
	public void parse(TiledMap map) throws IOException{
		this.map = map;
		props = map.getProperties();
		layer =  (TiledMapTileLayer)map.getLayers().get(0);
		tilesX = props.get("width", Integer.class);
		tilesY = props.get("height", Integer.class);
		tileWidth = props.get("tilewidth", Integer.class);
		tileHeight = props.get("tileheight", Integer.class);
		Dimension size = getEngine().getScreenSize();
		camera = new OrthographicCamera();
		camera.setToOrtho(true, size.width, size.height);
		encode();
	}
	
	@Override
	public int getWidth() {
		return tilesX * tileWidth;
	}
	
	@Override
	public int getHeight() {
		return tilesY * tileHeight;
	}
	
	@Override
	public Tile tileAt(int x, int y) {
		if(outOfBounds(x, y))
			return Tile.HOLLOW;
		
		int tileX = x / tileWidth;
		int tileY = y / tileHeight;
		
		Cell cell = layer.getCell(tileX, tileY);
		if(cell != null){
			TextureRegion region = cell.getTile().getTextureRegion();
			int regX = region.getRegionX();
			int regY = region.getRegionY();
			int relX = x % tileWidth;
			int relY = y % tileHeight;
			regY -= tileHeight;
			
			return (tileSet.getPixel(regX + relX, regY + relY) & 0x000000FF) > 0 ? Tile.SOLID : Tile.HOLLOW;
		}
		else
			return Tile.HOLLOW;
	}
	
	@Override
	public boolean isHollow(int x, int y) {
		return tileAt(x,y) == Tile.HOLLOW;
	}
	
	@Override
	public boolean isSolid(int x, int y) {
		return tileAt(x,y) == Tile.SOLID;
	}
	
	@Override
	public void setTileOnLayer(int x, int y, Tile tile) {
		int key = x * 31 + y;
		
		if(tile == null)
			tileLayer.remove(key);
		else
			tileLayer.put(key, PixelBasedLevel.mapToByte(tile));		
	}
	
	@Override
	public void removeTileOnLayer(int x, int y) {
		tileLayer.remove(x * 31 + y);		
	}
	
	@Override
	public void clearTileLayer() {
		tileLayer.clear();
	}
	
	public void setTile(int tileX, int tileY, Cell cell){
		if(tileX < 0 || tileX > tilesX || tileY < 0 || tileY > tilesY)
			return;
		
		Cell org = layer.getCell(tileX, tileY);
		layer.setCell(tileX, tileY, cell);
		
		int key = tileX * 31 + tileY;
		if(orgTiles.get(key) == null)
			orgTiles.put(key, new Holder(org,tileX,tileY));
	}
	
	public void transformTiles(int tileCx, int tileCy, int radius, Cell cell) {
	    int rr = radius*radius;

	    for(int x = tileCx - radius; x <= tileCx + radius; ++x){
	        for(int y = tileCy - radius; y <= tileCy + radius; ++y){
	            int dx = tileCx - x;
	            int dy = tileCy - y;
	            if((dx*dx + dy*dy) < rr)
	                setTile(x, y, cell);
	         }
	    }
	}
	
	public void restoreTiles(){
		orgTiles.values().forEach(holder->{
			layer.setCell(holder.x, holder.y, holder.cell);
		});
		orgTiles.clear();
	}
	
	public int getTileWidth(){
		return tileWidth;
	}
	
	public int getTileHeight(){
		return tileHeight;
	}
	
	public Entity getWorldImage(){
		if(worldImage == null){
			worldImage = new Entity(){{
				zIndex(100);
			}
				@Override
				public void render(SpriteBatch batch) {
					if(tiledMapRenderer == null)
						tiledMapRenderer = new OrthogonalTiledMapRenderer(map, batch);

					if(getRotation() != 0 || flipX || flipY)
						throw new RuntimeException("Rotation and flip are not supported for tile based image.");

					Color color = batch.getColor();
					Color newColor = new Color(color);
					newColor.a = alpha;

					batch.setColor(newColor);
					
					camera.position.x = getEngine().tx();
					camera.position.y = getEngine().ty();
					camera.zoom = getEngine().getZoom();
					camera.rotate(-rotation);
					camera.rotate((rotation = getEngine().getRotation()));
					camera.update();
					
					tiledMapRenderer.setView(camera);
					tiledMapRenderer.renderTileLayer(layer);

					batch.setColor(color);
					getEngine().gameCamera();
				}
			};
		}
		
		return worldImage;
	}
	
	@Override
	protected void clean() {
		super.clean();
		restoreTiles();
	}
	
	private void encode() throws IOException{
		for(int x = 0; x < tilesX; x++){
			for(int y = 0; y < tilesY; y++){
				Cell cell = layer.getCell(x, y);
				if(cell != null){
					TextureRegion region = cell.getTile().getTextureRegion();
					TextureData tdata = region.getTexture().getTextureData();
					tdata.prepare();
					Pixmap pix = tdata.consumePixmap();
					tileSet = new Image2D(pix, true);
					pix.dispose();
					return;
				}
			}
		}
	}

	private static class Holder{
		Cell cell;
		int x,y;
		
		Holder(Cell cell, int x, int y) {
			this.cell = cell;
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj) {
			Holder holder = (Holder)obj;
			return (x * 31 + y) == (holder.x * 31 + holder.y);
		}
	}
}