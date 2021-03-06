package com.schoenberger.gdx.game;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.schoenberger.gdx.util.CameraHelper;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.schoenberger.gdx.game.objects.Rock;
import com.schoenberger.gdx.util.Constants;
import com.badlogic.gdx.math.Rectangle;
import com.schoenberger.gdx.game.objects.BunnyHead;
import com.schoenberger.gdx.game.objects.BunnyHead.JUMP_STATE;
import com.schoenberger.gdx.game.objects.Feather;
import com.schoenberger.gdx.game.objects.GoldCoin;
import com.schoenberger.gdx.game.objects.Rock;
import com.badlogic.gdx.Game;
import com.schoenberger.gdx.game.screens.MenuScreen;
import com.schoenberger.gdx.util.AudioManager;

public class WorldController extends InputAdapter{
	// Tags are required for all debug messages
	private static final String TAG = WorldController.class.getName();

	public float livesVisual;
	public float scoreVisual;
	
	public Level level;
	public int lives;
	public int score;

	// Rectangles for collision detection
	private Rectangle r1 = new Rectangle ();
	private Rectangle r2 = new Rectangle ();

	private Game game;

	private void backToMenu () {
		// switch to menu screen
		game.setScreen(new MenuScreen(game));
	}

	private void onCollisionBunnyHeadWithRock (Rock rock) {
		BunnyHead bunnyHead = level.bunnyHead;
		float heightDifference = Math.abs(bunnyHead.position.y
				- (rock.position.y + rock.bounds.height));
		if (heightDifference > .25f) {
			boolean hitRightEdge = bunnyHead.position.x > (
					rock.position.x + rock.bounds.width /2.0f);
			if (hitRightEdge) {
				bunnyHead.position.x = rock.position.x + rock.bounds.width;
			} else {
				bunnyHead.position.x = rock.position.x -
						bunnyHead.bounds.width;
			}
			return;
		}

		switch (bunnyHead.jumpState) {
		case GROUNDED:
			break;
		case FALLING:
		case JUMP_FALLING:
			bunnyHead.position.y = rock.position.y +
			bunnyHead.bounds.height + bunnyHead.origin.y;
			bunnyHead.jumpState = JUMP_STATE.GROUNDED;
			break;
		case JUMP_RISING:
			bunnyHead.position.y = rock.position.y +
			bunnyHead.bounds.height + bunnyHead.origin.y;
			break;
		}
	}

	private void onCollisionBunnyWithGoldCoin (GoldCoin goldcoin) {
		goldcoin.collected = true;
		AudioManager.instance.play(Assets.instance.sounds.pickupCoin);
		score += goldcoin.getScore();
		Gdx.app.log(TAG,  "Gold coin collected");
	}

	private void onCollisionBunnyWithFeather( Feather feather) {
		feather.collected = true;
		AudioManager.instance.play(Assets.instance.sounds.pickupFeather);
		score += feather.getScore();
		level.bunnyHead.setFeatherPowerup(true);
		Gdx.app.log(TAG, "Feather collected");
	}

	private void testCollisions () {
		r1.set(level.bunnyHead.position.x, level.bunnyHead.position.y,
				level.bunnyHead.bounds.width, level.bunnyHead.bounds.height);

		// Test Collision: Bunny Head <-> Rocks
		for (Rock rock : level.rocks) {
			r2.set(rock.position.x, rock.position.y, rock.bounds.width,
					+rock.bounds.height);
			if (!r1.overlaps(r2)) continue;
			onCollisionBunnyHeadWithRock(rock);
			// IMPORTANT: must do all collision for valid
			// edge testing on rocks
		}

		// Test collision: Bunny head <->  Gold Coins
		for (GoldCoin goldCoin : level.goldCoins) {
			if (goldCoin.collected) continue;
			r2.set(goldCoin.position.x, goldCoin.position.y,
					goldCoin.bounds.width, goldCoin.bounds.height);
			if (!r1.overlaps(r2)) continue;
			onCollisionBunnyWithGoldCoin(goldCoin);
			break;
		}

		// Test collision: Bunny Head <-> Feathers
		for (Feather feather : level.feathers) {
			if (feather.collected) continue;
			r2.set(feather.position.x, feather.position.y,
					feather.bounds.width, feather.bounds.height);
			if (!r1.overlaps(r2)) continue;
			onCollisionBunnyWithFeather(feather);
			break;
		}
	}

	public CameraHelper cameraHelper;

	public WorldController() {
		init();
	}

	public WorldController(Game game) {
		this.game = game;
		init();
	}

	private void init() {
		Gdx.input.setInputProcessor(this);
		cameraHelper = new CameraHelper();
		lives = Constants.LIVES_START;
		livesVisual = lives;
		timeLeftGameOverDelay = 0;
		initLevel();
	}

	private void initLevel() {
		score = 0;
		scoreVisual = score;
		level = new Level(Constants.LEVEL_01);
		cameraHelper.setTarget(level.bunnyHead);
	}

	/**
	 * Draws a fancy test asset (a cyan box filled with red and with a yellow x)
	 * @param width
	 * @param height
	 * @return
	 */
	private Pixmap createProceduralPixmap (int width, int height) {
		Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		// Fill square with red color at 50% opacity
		pixmap.setColor(1, 0, 0, 0.5f);
		pixmap.fill();
		// Draw a yellow-colored X shape on the square
		pixmap.setColor(1, 1, 0, 1);
		pixmap.drawLine(0, 0, width, height);
		pixmap.drawLine(width, 0, 0, height);
		// Draw a cyan-colored border around square
		pixmap.setColor(0, 1, 1, 1);
		pixmap.drawRectangle(0,  0, width, height);
		return pixmap;
	}

	public void update(float deltaTime) {
		handleDebugInput(deltaTime);
		if (isGameOver()) {
			timeLeftGameOverDelay -= deltaTime;
			if (timeLeftGameOverDelay < 0) backToMenu();
		} else {
		handleInputGame(deltaTime);
		}
		level.update(deltaTime);
		testCollisions();
		cameraHelper.update(deltaTime);
		if (!isGameOver() && isPlayerInWater()) 
		{
			AudioManager.instance.play(Assets.instance.sounds.liveLost);
			lives--;
			if (isGameOver())
				timeLeftGameOverDelay = Constants.TIME_DELAY_GAME_OVER;
			else
				initLevel();
		}
		level.mountains.updateScrollPosition
		(cameraHelper.getPosition());
		if (livesVisual > lives)
			livesVisual = Math.max(lives, livesVisual-1*deltaTime);
		if (scoreVisual < score)
			scoreVisual = Math.min(score, scoreVisual + 250 * deltaTime);
	}

	private void handleDebugInput (float deltaTime) {
		if (Gdx.app.getType() != ApplicationType.Desktop) return;

		if (!cameraHelper.hasTarget(level.bunnyHead)){
			// Camera Controls (move)
			float camMoveSpeed = 5 * deltaTime;
			float camMoveSpeedAccelerationFactor = 5;
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) camMoveSpeed *=
					camMoveSpeedAccelerationFactor;
			if (Gdx.input.isKeyPressed(Keys.LEFT)) moveCamera(-camMoveSpeed, 0);
			if (Gdx.input.isKeyPressed(Keys.RIGHT)) moveCamera(camMoveSpeed, 0);
			if (Gdx.input.isKeyPressed(Keys.UP)) moveCamera (0, camMoveSpeed);
			if (Gdx.input.isKeyPressed(Keys.DOWN)) moveCamera(0, -camMoveSpeed);
			if (Gdx.input.isKeyPressed(Keys.BACKSPACE)) cameraHelper.setPosition(0, 0);
		}
		// Camera Controls (zoom)
		float camZoomSpeed = 1 * deltaTime;
		float camZoomSpeedAccelerationFactor = 5;
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) camZoomSpeed *=
				camZoomSpeedAccelerationFactor;
		if (Gdx.input.isKeyPressed(Keys.COMMA)) cameraHelper.addZoom(camZoomSpeed);
		if (Gdx.input.isKeyPressed(Keys.PERIOD)) cameraHelper.addZoom(-camZoomSpeed);
		if (Gdx.input.isKeyPressed(Keys.SLASH)) cameraHelper.setZoom(1);

	}

	private void moveCamera (float x, float y) {
		x += cameraHelper.getPosition().x;
		y += cameraHelper.getPosition().y;
		cameraHelper.setPosition(x, y);
	}

	private void handleInputGame(float dletaTime) {
		if (cameraHelper.hasTarget(level.bunnyHead)) {
			// Player Movement
			if (Gdx.input.isKeyPressed(Keys.LEFT)) {
				level.bunnyHead.velocity.x =
						-level.bunnyHead.terminalVelocity.x;
			} else if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
				level.bunnyHead.velocity.x =
						level.bunnyHead.terminalVelocity.x;
			} else {
				// Execute auto-forward movement on non-desktop platofmr
				if (Gdx.app.getType() != ApplicationType.Desktop) {
					level.bunnyHead.velocity.x =
							level.bunnyHead.terminalVelocity.x;
				}
			}

			// Bunny Jump
			if (Gdx.input.isTouched() || Gdx.input.isKeyPressed(Keys.SPACE)) {
				level.bunnyHead.setJumping(true);
			} else {
				level.bunnyHead.setJumping(false);
			}
		}
	}

	@Override
	public boolean keyUp (int keycode) {
		//Reset game world
		if (keycode == Keys.R) {
			init();
			Gdx.app.debug(TAG, "Game world was reset"); // "Resetted" isn't a word, so I changed the log message a bit.
		}
		// Toggle camera follow
		else if (keycode == Keys.ENTER) {
			cameraHelper.setTarget(cameraHelper.hasTarget()
					? null: level.bunnyHead);
			Gdx.app.debug(TAG,  "Camera follow enabled: "
					+ cameraHelper.hasTarget());
		}
		// Back to menu
		else if (keycode == Keys.ESCAPE || keycode == Keys.BACK) {
			backToMenu();
		}
		return false;
	}

	private float timeLeftGameOverDelay;

	public boolean isGameOver() {
		return lives < 0;
	}

	public boolean isPlayerInWater() {
		return level.bunnyHead.position.y < -5;
	}
}
