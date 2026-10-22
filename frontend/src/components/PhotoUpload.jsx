import { useEffect, useRef, useState } from 'react';
import { ImagePlus, Plus, X } from 'lucide-react';
import { mediaUrl } from '../api/client';

function PhotoPreview({ file }) {
  const imageRef = useRef(null);

  useEffect(() => {
    const url = URL.createObjectURL(file);
    imageRef.current.src = url;
    return () => URL.revokeObjectURL(url);
  }, [file]);

  return <img ref={imageRef} alt={file.name} />;
}

export default function PhotoUpload({ files, onChange, disabled = false, existing = [], onRemoveExisting }) {
  const inputRef = useRef(null);
  const [error, setError] = useState('');

  // Section: Product photos — accept images and skip duplicate selections.
  const addPhotos = (event) => {
    const selected = Array.from(event.target.files || []);
    const valid = selected.filter((file) => file.type.startsWith('image/') && file.size > 0);
    setError(valid.length !== selected.length ? 'Please choose image files. Empty files and other file types were skipped.' : '');
    const next = [...files];
    valid.forEach((file) => {
      if (!next.some((existing) => existing.name === file.name && existing.size === file.size && existing.lastModified === file.lastModified)) {
        next.push(file);
      }
    });
    onChange(next);
    // Allow choosing the same file again after removing its preview.
    event.target.value = '';
  };

  return (
    <fieldset className="photo-upload" disabled={disabled}>
      <legend>Product photos</legend>
      <p className="photo-upload-help" id="product-photos-help">Add clear photos of your item. The first photo will be the cover.</p>
      <input ref={inputRef} type="file" accept="image/*" multiple hidden onChange={addPhotos} aria-label="Choose product photos" />
      <div className={`photo-upload-grid ${files.length + existing.length ? 'has-photos' : ''}`}>
        {existing.map((photo, index) => (
          <div className="photo-upload-preview" key={photo.id}>
            <img src={mediaUrl(photo.image)} alt={`Saved product photo ${index + 1}`} />
            {index === 0 && <span className="photo-cover-label">Cover photo</span>}
            <button type="button" className="photo-remove" aria-label={`Remove saved photo ${index + 1}`} onClick={() => onRemoveExisting(photo.id)}><X size={15} /></button>
            <span className="photo-filename">Saved photo {index + 1}</span>
          </div>
        ))}
        {files.map((file, index) => (
          <div className="photo-upload-preview" key={`${file.name}-${file.size}-${file.lastModified}`}>
            <PhotoPreview file={file} />
            {index === 0 && !existing.length && <span className="photo-cover-label">Cover photo</span>}
            <button type="button" className="photo-remove" aria-label={`Remove ${file.name}`} onClick={() => onChange(files.filter((_, i) => i !== index))}>
              <X size={15} />
            </button>
            <span className="photo-filename" title={file.name}>{file.name}</span>
          </div>
        ))}
        <button type="button" className="photo-add-box" aria-describedby="product-photos-help" onClick={() => inputRef.current.click()}>
          <span className="photo-add-icon">{files.length ? <Plus size={25} /> : <ImagePlus size={29} strokeWidth={1.6} />}</span>
          <strong>{files.length + existing.length ? 'Add more photos' : 'Add photos'}</strong>
          <span>{files.length ? 'Choose images' : 'Click to choose images from your device'}</span>
        </button>
      </div>
      <p className="photo-upload-count" role="status">{files.length + existing.length ? `${files.length + existing.length} photo${files.length + existing.length === 1 ? '' : 's'} selected` : 'You can select multiple photos.'}</p>
      {error && <p className="form-error" role="alert">{error}</p>}
    </fieldset>
  );
}
