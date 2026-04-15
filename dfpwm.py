#!/usr/bin/env python3
"""
DFPWM Conversion Server for ComputerCraft
Requires: flask, ffmpeg-python
Install: pip install flask ffmpeg-python
Run: python server.py
"""

from flask import Flask, request, send_file, jsonify
from flask_cors import CORS
import ffmpeg
import os
import tempfile
import uuid

app = Flask(__name__)
CORS(app)  # Allow requests from ComputerCraft

@app.route('/', methods=['POST'])
def convert_audio():
    """Convert uploaded audio to DFPWM format"""

    # Check if file was uploaded
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'}), 400

    file = request.files['file']

    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400

    # Create temporary files
    temp_id = str(uuid.uuid4())
    input_path = os.path.join(tempfile.gettempdir(), f"{temp_id}_input")
    output_path = os.path.join(tempfile.gettempdir(), f"{temp_id}_output.dfpwm")

    try:
        # Save uploaded file
        file.save(input_path)

        # Convert to DFPWM using ffmpeg
        # -ac 1: mono audio
        # -ar 48000: 48kHz sample rate (CC standard)
        # -f dfpwm: output format
        (
            ffmpeg
            .input(input_path)
            .output(output_path, format='dfpwm', ar=48000, ac=1)
            .overwrite_output()
            .run(capture_stdout=True, capture_stderr=True)
        )

        # Send the converted file
        return send_file(
            output_path,
            mimetype='application/octet-stream',
            as_attachment=True,
            download_name='converted.dfpwm'
        )

    except ffmpeg.Error as e:
        return jsonify({
            'error': 'Conversion failed',
            'details': e.stderr.decode()
        }), 500

    finally:
        # Cleanup temporary files
        for path in [input_path, output_path]:
            if os.path.exists(path):
                try:
                    os.remove(path)
                except:
                    pass

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'ok', 'service': 'DFPWM Converter'})

if __name__ == '__main__':
    print("DFPWM Conversion Server Starting...")
    print("Access at: http://localhost:5000")
    print("For ComputerCraft, use your local IP address")
    print("(Find it with 'ipconfig' on Windows or 'ifconfig' on Linux/Mac)")
    app.run(host='0.0.0.0', port=5000, debug=True)
