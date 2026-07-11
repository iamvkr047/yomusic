export async function compressImage(imageUrl: string): Promise<string> {
  // Fetch the image data
  const response = await fetch(imageUrl);
  const blob = await response.blob();

  // 300KB in bytes = 300 * 1024 = 307200
  const sizeLimitBytes = 307200;

  if (blob.size <= sizeLimitBytes) {
    // FAST PATH: File is small enough. Convert directly to Base64 without canvas
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => {
        resolve(reader.result as string);
      };
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  } else {
    return new Promise((resolve) => {
      const img = new Image();
      img.crossOrigin = 'anonymous'; // Avoid CORS issues
      img.src = imageUrl;

      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');

        // Set target thumbnail dimensions (e.g., max 300px width)
        const scale = 300 / img.width;
        canvas.width = 300;
        canvas.height = img.height * scale;

        ctx!.drawImage(img, 0, 0, canvas.width, canvas.height);

        // Compress quality to 60% (0.6) -> drops file size drastically!
        const compressedBase64 = canvas.toDataURL('image/jpeg', 0.6);
        resolve(compressedBase64);
      };
    });
  }
}
