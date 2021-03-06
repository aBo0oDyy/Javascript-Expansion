/*
 *
 * Javascript-Expansion
 * Copyright (C) 2019 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.javascript;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandMap;

public class JavascriptExpansion extends PlaceholderExpansion implements Cacheable, Configurable {
	
	private ScriptEngine globalEngine = null;
	
	private JavascriptPlaceholdersConfig config;
	
	private final Set<JavascriptPlaceholder> scripts = new HashSet<>();
	
	private final String VERSION = getClass().getPackage().getImplementationVersion();
	
	private static JavascriptExpansion instance;

	private CommandMap cmdMap = null;
	
	public JavascriptExpansion() {
		instance = this;
	}

	@Override
	public String getAuthor() {
		return "clip";
	}

	@Override
	public String getIdentifier() {
		return "javascript";
	}
	
	@Override
	public String getVersion() {
		return VERSION;
	}
	
	@Override
	public boolean register() {
        this.getGlobalEngine();

		try {
			final Field commandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			commandMap.setAccessible(true);
			cmdMap = (CommandMap) commandMap.get(Bukkit.getServer());

			cmdMap.register("placeholderapi", new JavascriptCommands(this));
		} catch(Exception e) {
			e.printStackTrace();
		}

		config = new JavascriptPlaceholdersConfig(this);
        int l = reloadScripts();
        getPlaceholderAPI().getLogger().info("" + l + " script" + (l == 1 ? "" : "s")+ " loaded.");
		return super.register();
	}
	
	@Override
	public void clear() {
		scripts.forEach(s -> {
			s.saveData();
			s.cleanup();
		});
		scripts.clear();
		globalEngine = null;
		instance = null;
	}

	@Override
	public String onRequest(OfflinePlayer p, String identifier) {
		if (p == null) {
			return "";
		}
		
		if (scripts.isEmpty()) {
			return null;
		}

		for (JavascriptPlaceholder script : scripts) {
			if (identifier.startsWith(script.getIdentifier() + "_")) {
				identifier = identifier.replace(script.getIdentifier() + "_", "");
				return !identifier.contains(",") ? script.evaluate(p, identifier) : script.evaluate(p, identifier.split(","));
			} else if (identifier.equalsIgnoreCase(script.getIdentifier())) {
				return script.evaluate(p);
			}
		}
		return null;
	}
	
	public boolean addJSPlaceholder(JavascriptPlaceholder p) {
		if (p == null) {
			return false;
		}
		
		if (scripts.isEmpty()) {
			scripts.add(p);
			return true;
		}
		
		if (scripts.stream().filter(s -> s.getIdentifier().equalsIgnoreCase(p.getIdentifier())).findFirst().orElse(null) != null) {
			return false;
		}
		
		scripts.add(p);
		return true;
	}
	
	public Set<JavascriptPlaceholder> getJSPlaceholders() {
		return scripts;
	}
	
	public List<String> getLoadedIdentifiers() {
		List<String> l = new ArrayList<>();
		scripts.forEach(s -> {
			l.add(s.getIdentifier());
		});
		return l;
	}
	
	public JavascriptPlaceholder getJSPlaceholder(String identifier) {
		return scripts.stream().filter(s -> s.getIdentifier().equalsIgnoreCase(identifier)).findFirst().orElse(null);
	}
	
	public int getAmountLoaded() {
		return scripts.size();
	}
	
	public ScriptEngine getGlobalEngine() {
        if (globalEngine == null) {
            try {
                globalEngine = new ScriptEngineManager().getEngineByName(getString("engine", "javascript"));
            } catch (NullPointerException ex) {
                getPlaceholderAPI().getLogger().warning("Javascript engine type was invalid! Defaulting to 'javascript'");
                globalEngine = new ScriptEngineManager().getEngineByName("javascript");
            }
        }
		return globalEngine;
	}

	@Override
	public Map<String, Object> getDefaults() {
		Map<String, Object> def = new HashMap<>();
		def.put("engine", "javascript");
		return def;
	}
	
	public int reloadScripts() {
		scripts.forEach(s -> {
			s.saveData();
			s.cleanup();
		});
		scripts.clear();
		config.reload();
		return config.loadPlaceholders();
	}
	
	public static JavascriptExpansion getInstance() {
		return instance;
	}
}
