from argparse import ArgumentParser
import codecs
import csv
import os
import shutil
import tempfile
import time

import sino_transform

# Field order for the output files
STOPS_OUT_FIELDS = ['stop_id', 'stop_code', 'stop_name', 'stop_desc', 'stop_lat',
                    'stop_lon', 'zone_id', 'stop_url', 'location_type',
                    'parent_station', 'stop_timezone', 'wheelchair_boarding']

SHAPES_OUT_FIELDS = ['shape_id', 'shape_pt_lat', 'shape_pt_lon',
                     'shape_pt_sequence', 'shape_dist_traveled']


# Idea adapted from http://stackoverflow.com/questions/20899939/removing-bom-from-gziped-csv-in-python
# CSV files can have BOMs, but the Python csv module can't handle Unicode data, so the standard
# 'codecs.open('utf-8-sig')' won't work without re-encoding every string back to utf-8.
# This avoids that by stripping the BOM from the first line and proceeding as usual.
def remove_bom_from_first(iterable):
    first = True
    for line in iterable:
        if first:
            first = False
            if line.startswith(codecs.BOM_UTF8) and line is not None:
                yield line[3:]
        yield line


def correct_china_offset(csv_reader, lat_field, lng_field):
    """Corrects the Chinese GPS offsets for the specified csv_reader.

    Params:
        :csv_line: A dict / array of strings representing the CSV line.
        :lat_field: The field to look in for the latitude
        :lng_field: The field to look in for the longitude

    Yields: An iterable with the latitude and longitude fields corrected.
    """
    for csv_line in csv_reader:
        lat = csv_line[lat_field]
        lng = csv_line[lng_field]
        new_lat, new_lng = sino_transform.transform(float(lat), float(lng))
        csv_line[lat_field] = unicode(new_lat).encode('utf-8')
        csv_line[lng_field] = unicode(new_lng).encode('utf-8')

        yield csv_line


def correct_offset_in_file(file_path, lat_field, lng_field, out_fields):
    """Generates a temp file with corrected coordinates for the given file_path.

    Params:
        :file_path: Path to the file whose coordinates should be corrected.
        :lat_field: Name of the field in which latitude coordinates are found.
        :lng_field: Name of the field in which longitude coordinates are found.
        :out_fields: Names (in order) of the fields which should be written in the output.
    Returns:
        Path to generated temp file.
    """
    temp_file = tempfile.NamedTemporaryFile(delete=False)
    with open(file_path, 'rb') as csv_file:
        csv_reader = csv.DictReader(remove_bom_from_first(csv_file))
        csv_reader.next()  # Skip header row; not sure why DictReader doesn't do this.
        corrected_rows = correct_china_offset(csv_reader, lat_field, lng_field)
        csv_writer = csv.DictWriter(temp_file, out_fields)
        csv_writer.writeheader()
        csv_writer.writerows(corrected_rows)
        temp_file.close()

    return temp_file.name


def correct_china_gtfs(root_folder):
    """Corrects the coordinates in a GTFS feed with CSV files in root_folder.

    Params:
        :root_folder: Path to the folder where the feed's CSV files are."""

    # Stops and Shapes are the only two files which can contain lat/lon data
    # that needs to be corrected.
    stops_temp_file = correct_offset_in_file(os.path.join(root_folder, 'stops.txt'),
                                             'stop_lat',
                                             'stop_lon',
                                             STOPS_OUT_FIELDS)

    shapes_temp_file = correct_offset_in_file(os.path.join(root_folder, 'shapes.txt'),
                                              'shape_pt_lat',
                                              'shape_pt_lon',
                                              SHAPES_OUT_FIELDS)

    # Move old stops.txt and shapes.txt out of the way
    shutil.move(os.path.join(root_folder, 'stops.txt'),
                os.path.join(root_folder, 'stops%s.txt' % time.time()))
    shutil.move(os.path.join(root_folder, 'shapes.txt'),
                os.path.join(root_folder, 'shapes%s.txt' % time.time()))
    # Move corrected files into place.
    shutil.move(stops_temp_file, os.path.join(root_folder, 'stops.txt'))
    shutil.move(shapes_temp_file, os.path.join(root_folder, 'shapes.txt'))


def main():
    desc = 'Corrects coordinates in a GTFS feed located in China.'
    parser = ArgumentParser(description=desc)
    parser.add_argument('gtfs_root', help='Folder containing GTFS feed .txt files.')
    args = parser.parse_args()
    correct_china_gtfs(args.gtfs_root)

if __name__ == '__main__':
    main()
