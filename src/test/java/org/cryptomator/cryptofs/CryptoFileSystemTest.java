package org.cryptomator.cryptofs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.cryptomator.cryptolib.CryptorProvider;
import org.cryptomator.cryptolib.InvalidPassphraseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class CryptoFileSystemTest {

	private static final SecureRandom NULL_RANDOM = new SecureRandom() {
		@Override
		public synchronized void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		};
	};
	private static final CryptorProvider NULL_CRYPTOR_PROVIDER = new CryptorProvider(NULL_RANDOM);

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Path tmpPath;
	private ConcurrentHashMap<Path, CryptoFileSystem> openFileSystems;
	private CryptoFileSystemProvider provider;

	@Before
	public void setup() throws IOException, ReflectiveOperationException {
		tmpPath = Files.createTempDirectory("unit-tests");
		openFileSystems = new ConcurrentHashMap<>();
		provider = Mockito.mock(CryptoFileSystemProvider.class);
		Mockito.when(provider.getFileSystems()).thenReturn(openFileSystems);
	}

	@After
	public void teardown() throws IOException {
		Files.walkFileTree(tmpPath, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	@Test
	public void testConstructorForNewVault() throws IOException {
		CryptoFileSystem fs = new CryptoFileSystem(provider, NULL_CRYPTOR_PROVIDER, tmpPath, "foo");
		fs.close();
	}

	@Test
	public void testConstructorForExistingVault() throws IOException {
		CryptoFileSystem fs = new CryptoFileSystem(provider, NULL_CRYPTOR_PROVIDER, tmpPath, "foo");
		fs.close();

		CryptoFileSystem fs2 = new CryptoFileSystem(provider, NULL_CRYPTOR_PROVIDER, tmpPath, "foo");
		fs2.close();
	}

	@Test
	public void testConstructorForExistingVaultWithWrongPw() throws IOException {
		CryptoFileSystem fs = new CryptoFileSystem(provider, NULL_CRYPTOR_PROVIDER, tmpPath, "foo");
		fs.close();

		thrown.expect(InvalidPassphraseException.class);
		CryptoFileSystem fs2 = new CryptoFileSystem(provider, NULL_CRYPTOR_PROVIDER, tmpPath, "bar");
		fs2.close();
	}

}
