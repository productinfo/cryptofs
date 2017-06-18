/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.cryptofs.migration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;

import org.cryptomator.cryptofs.migration.api.Migrator;
import org.cryptomator.cryptofs.migration.api.NoApplicableMigratorException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MigratorsTest {

	private ByteBuffer keyFile;
	private Path pathToVault;

	@Before
	public void setup() throws IOException {
		keyFile = StandardCharsets.UTF_8.encode("{\"version\": 0000}");
		pathToVault = Mockito.mock(Path.class);

		Path pathToMasterkey = Mockito.mock(Path.class);
		FileSystem fs = Mockito.mock(FileSystem.class);
		FileSystemProvider provider = Mockito.mock(FileSystemProvider.class);
		SeekableByteChannel sbc = Mockito.mock(SeekableByteChannel.class);

		Mockito.when(pathToVault.resolve("masterkey.cryptomator")).thenReturn(pathToMasterkey);
		Mockito.when(pathToMasterkey.getFileSystem()).thenReturn(fs);
		Mockito.when(fs.provider()).thenReturn(provider);
		Mockito.when(provider.newByteChannel(Mockito.eq(pathToMasterkey), Mockito.any(), Mockito.any())).thenReturn(sbc);
		Mockito.when(sbc.size()).thenReturn((long) keyFile.remaining());
		Mockito.when(sbc.read(Mockito.any())).then(invocation -> {
			ByteBuffer dst = invocation.getArgument(0);
			int n = Math.min(keyFile.remaining(), dst.remaining());
			byte[] tmp = new byte[n];
			keyFile.get(tmp);
			dst.put(tmp);
			return n;
		});
	}

	@Test
	public void testNeedsMigration() throws IOException {
		Migrators migrators = new Migrators(Collections.emptyMap());
		boolean result = migrators.needsMigration(pathToVault, "masterkey.cryptomator");

		Assert.assertTrue(result);
	}

	@Test
	public void testNeedsNoMigration() throws IOException {
		keyFile = StandardCharsets.UTF_8.encode("{\"version\": 9999}");

		Migrators migrators = new Migrators(Collections.emptyMap());
		boolean result = migrators.needsMigration(pathToVault, "masterkey.cryptomator");

		Assert.assertFalse(result);
	}

	@Test(expected = NoApplicableMigratorException.class)
	public void testMigrateWithoutMigrators() throws IOException {
		Migrators migrators = new Migrators(Collections.emptyMap());
		migrators.migrate(pathToVault, "masterkey.cryptomator", "secret");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testMigrate() throws NoApplicableMigratorException, InvalidPassphraseException, IOException {
		Migrator migrator = Mockito.mock(Migrator.class);
		Migrators migrators = new Migrators(new HashMap<Migration, Migrator>() {
			{
				put(Migration.ZERO_TO_ONE, migrator);
			}
		});
		migrators.migrate(pathToVault, "masterkey.cryptomator", "secret");
		Mockito.verify(migrator).migrate(pathToVault, "masterkey.cryptomator", "secret");
	}

}
